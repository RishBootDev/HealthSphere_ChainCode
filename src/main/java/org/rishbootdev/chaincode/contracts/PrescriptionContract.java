package org.rishbootdev.chaincode.contracts;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.rishbootdev.chaincode.model.Prescription;
import org.rishbootdev.chaincode.model.Medicine;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;

import java.util.ArrayList;
import java.util.List;

@Contract(name = "PrescriptionContract")
public class PrescriptionContract {

    private final Gson gson = new Gson();
    private static final String PRESC_PREFIX = "PRESC_";
    private static final String MEDICINE_PREFIX = "MEDICINE_";

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public String createPrescription(Context ctx, String prescriptionJson) {
        ChaincodeStub stub = ctx.getStub();
        Prescription prescription = gson.fromJson(prescriptionJson, Prescription.class);

        if (prescription.getPrescriptionId() == null || prescription.getPrescriptionId().isEmpty()) {
            throw new RuntimeException("Prescription ID cannot be empty");
        }

        String key = PRESC_PREFIX + prescription.getPrescriptionId();
        if (!stub.getStringState(key).isEmpty()) {
            throw new RuntimeException("Prescription already exists: " + prescription.getPrescriptionId());
        }

        stub.putStringState(key, gson.toJson(prescription));
        return "Prescription created successfully for patient: " + prescription.getPatientId();
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String getPrescriptionById(Context ctx, String prescriptionId) {
        ChaincodeStub stub = ctx.getStub();
        String key = PRESC_PREFIX + prescriptionId;

        String json = stub.getStringState(key);
        if (json == null || json.isEmpty()) {
            throw new RuntimeException("Prescription not found: " + prescriptionId);
        }

        return json;
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String getAllPrescriptions(Context ctx) {
        ChaincodeStub stub = ctx.getStub();
        List<Prescription> prescriptions = new ArrayList<>();

        try (QueryResultsIterator<KeyValue> results = stub.getStateByRange(PRESC_PREFIX, PRESC_PREFIX + "\uFFFF")) {
            for (KeyValue kv : results) {
                try {
                    Prescription p = gson.fromJson(kv.getStringValue(), Prescription.class);
                    if (p != null && p.getPrescriptionId() != null) {
                        prescriptions.add(p);
                    }
                } catch (JsonSyntaxException e) {
                    System.out.println("wrong json parsing");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error fetching prescriptions: " + e.getMessage());
        }

        return gson.toJson(prescriptions);
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public String updatePrescription(Context ctx, String prescriptionId, String updatedJson) {
        ChaincodeStub stub = ctx.getStub();
        String key = PRESC_PREFIX + prescriptionId;

        if (stub.getStringState(key).isEmpty()) {
            throw new RuntimeException("Prescription not found: " + prescriptionId);
        }

        Prescription updated = gson.fromJson(updatedJson, Prescription.class);
        stub.putStringState(key, gson.toJson(updated));
        return "Prescription updated: " + prescriptionId;
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public String deletePrescription(Context ctx, String prescriptionId) {
        ChaincodeStub stub = ctx.getStub();
        String key = PRESC_PREFIX + prescriptionId;

        if (stub.getStringState(key).isEmpty()) {
            throw new RuntimeException("Prescription not found: " + prescriptionId);
        }

        stub.delState(key);
        return "🗑️ Prescription deleted successfully: " + prescriptionId;
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String getPrescriptionsByPatient(Context ctx, String patientId) {
        ChaincodeStub stub = ctx.getStub();
        List<Prescription> prescriptions = new ArrayList<>();

        try (QueryResultsIterator<KeyValue> results = stub.getStateByRange(PRESC_PREFIX, PRESC_PREFIX + "\uFFFF")) {
            for (KeyValue kv : results) {
                Prescription p = gson.fromJson(kv.getStringValue(), Prescription.class);
                if (p != null && patientId.equals(p.getPatientId())) {
                    prescriptions.add(p);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error reading prescriptions: " + e.getMessage());
        }

        return gson.toJson(prescriptions);
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String getPrescriptionsByDoctor(Context ctx, String doctorId) {
        ChaincodeStub stub = ctx.getStub();
        List<Prescription> prescriptions = new ArrayList<>();

        try (QueryResultsIterator<KeyValue> results = stub.getStateByRange(PRESC_PREFIX, PRESC_PREFIX + "\uFFFF")) {
            for (KeyValue kv : results) {
                Prescription p = gson.fromJson(kv.getStringValue(), Prescription.class);
                if (p != null && doctorId.equals(p.getDoctorId())) {
                    prescriptions.add(p);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error reading prescriptions: " + e.getMessage());
        }

        return gson.toJson(prescriptions);
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String searchPrescriptions(Context ctx, String keyword) {
        ChaincodeStub stub = ctx.getStub();
        List<Prescription> prescriptions = new ArrayList<>();

        try (QueryResultsIterator<KeyValue> results = stub.getStateByRange(PRESC_PREFIX, PRESC_PREFIX + "\uFFFF")) {
            for (KeyValue kv : results) {
                Prescription p = gson.fromJson(kv.getStringValue(), Prescription.class);
                if (p != null) {
                    String data = gson.toJson(p).toLowerCase();
                    if (data.contains(keyword.toLowerCase())) {
                        prescriptions.add(p);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error searching prescriptions: " + e.getMessage());
        }

        return gson.toJson(prescriptions);
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String getMedicinesForPrescription(Context ctx, String prescriptionId) {
        ChaincodeStub stub = ctx.getStub();
        String json = stub.getStringState(PRESC_PREFIX + prescriptionId);

        if (json == null || json.isEmpty()) {
            throw new RuntimeException("Prescription not found: " + prescriptionId);
        }

        Prescription prescription = gson.fromJson(json, Prescription.class);
        List<String> medicineIds = prescription.getMedicineIdList();
        List<Medicine> medicines = new ArrayList<>();

        if (medicineIds == null || medicineIds.isEmpty()) {
            return "[]";
        }

        for (String medId : medicineIds) {
            String medJson = stub.getStringState(MEDICINE_PREFIX + medId);
            if (medJson != null && !medJson.isEmpty()) {
                try {
                    Medicine med = gson.fromJson(medJson, Medicine.class);
                    if (med != null) {
                        medicines.add(med);
                    }
                } catch (JsonSyntaxException ignore) {}
            }
        }

        return gson.toJson(medicines);
    }
}
