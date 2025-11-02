package org.rishbootdev.chaincode.contracts;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Default;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;
import org.rishbootdev.chaincode.model.Medicine;
import org.rishbootdev.chaincode.model.Prescription;

import java.util.ArrayList;
import java.util.List;

@Contract(name = "PrescriptionContract")
@Default
public class PrescriptionContract {

    private final Gson gson = new Gson();

    private static final String PRESC_PREFIX = "PRESC_";
    private static final String MEDICINE_PREFIX = "MEDICINE_";
    private static final String PATIENT_PREFIX = "PATIENT_";
    private static final String DOCTOR_PREFIX = "DOCTOR_";

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

        if (prescription.getMedicineIdList() == null) {
            prescription.setMedicineIdList(new ArrayList<>());
        }

        stub.putStringState(key, gson.toJson(prescription));
        return "Prescription created successfully: " + prescription.getPrescriptionId();
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

        try (QueryResultsIterator<KeyValue> results =
                     stub.getStateByRange(PRESC_PREFIX, PRESC_PREFIX + "\uFFFF")) {
            for (KeyValue kv : results) {
                try {
                    Prescription presc = gson.fromJson(kv.getStringValue(), Prescription.class);
                    if (presc != null && presc.getPrescriptionId() != null) {
                        prescriptions.add(presc);
                    }
                } catch (JsonSyntaxException ignored) {}
            }
        } catch (Exception e) {
            throw new RuntimeException("Error fetching prescriptions: " + e.getMessage());
        }

        return gson.toJson(prescriptions);
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public String updatePrescription(Context ctx, String prescriptionJson) {
        ChaincodeStub stub = ctx.getStub();
        Prescription prescription = gson.fromJson(prescriptionJson, Prescription.class);

        String key = PRESC_PREFIX + prescription.getPrescriptionId();
        if (stub.getStringState(key).isEmpty()) {
            throw new RuntimeException("Prescription not found: " + prescription.getPrescriptionId());
        }

        stub.putStringState(key, gson.toJson(prescription));
        return "🩺 Prescription updated successfully: " + prescription.getPrescriptionId();
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public String deletePrescription(Context ctx, String prescriptionId) {
        ChaincodeStub stub = ctx.getStub();
        String key = PRESC_PREFIX + prescriptionId;

        String existing = stub.getStringState(key);
        if (existing == null || existing.isEmpty()) {
            throw new RuntimeException("Prescription not found: " + prescriptionId);
        }

        Prescription presc = gson.fromJson(existing, Prescription.class);

        if (presc.getMedicineIdList() != null) {
            for (String medId : presc.getMedicineIdList()) {
                String medKey = MEDICINE_PREFIX + medId;
                String medJson = stub.getStringState(medKey);
                if (medJson != null && !medJson.isEmpty()) {
                    Medicine med = gson.fromJson(medJson, Medicine.class);
                    stub.putStringState(medKey, gson.toJson(med));
                }
            }
        }

        stub.delState(key);
        return "Prescription deleted: " + prescriptionId;
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void addMedicineToPrescription(Context ctx, String prescriptionId, String medicineId) {
        ChaincodeStub stub = ctx.getStub();
        String prescKey = PRESC_PREFIX + prescriptionId;
        String prescJson = stub.getStringState(prescKey);

        if (prescJson == null || prescJson.isEmpty()) {
            throw new RuntimeException("Prescription not found: " + prescriptionId);
        }

        String medKey = MEDICINE_PREFIX + medicineId;
        String medJson = stub.getStringState(medKey);
        if (medJson == null || medJson.isEmpty()) {
            throw new RuntimeException("Medicine not found: " + medicineId);
        }

        Prescription prescription = gson.fromJson(prescJson, Prescription.class);
        List<String> meds = prescription.getMedicineIdList();
        if (meds == null) meds = new ArrayList<>();

        if (!meds.contains(medicineId)) {
            meds.add(medicineId);
            prescription.setMedicineIdList(meds);
            stub.putStringState(prescKey, gson.toJson(prescription));
        }
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void removeMedicineFromPrescription(Context ctx, String prescriptionId, String medicineId) {
        ChaincodeStub stub = ctx.getStub();
        String prescKey = PRESC_PREFIX + prescriptionId;
        String prescJson = stub.getStringState(prescKey);

        if (prescJson == null || prescJson.isEmpty()) {
            throw new RuntimeException("Prescription not found: " + prescriptionId);
        }

        Prescription prescription = gson.fromJson(prescJson, Prescription.class);
        List<String> meds = prescription.getMedicineIdList();

        if (meds != null && meds.remove(medicineId)) {
            prescription.setMedicineIdList(meds);
            stub.putStringState(prescKey, gson.toJson(prescription));
        }
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String getMedicinesForPrescription(Context ctx, String prescriptionId) {
        ChaincodeStub stub = ctx.getStub();
        String prescJson = stub.getStringState(PRESC_PREFIX + prescriptionId);

        if (prescJson == null || prescJson.isEmpty()) {
            throw new RuntimeException("Prescription not found: " + prescriptionId);
        }

        Prescription prescription = gson.fromJson(prescJson, Prescription.class);
        List<String> medicineIds = prescription.getMedicineIdList();
        List<Medicine> medicines = new ArrayList<>();

        if (medicineIds != null) {
            for (String medId : medicineIds) {
                String medJson = stub.getStringState(MEDICINE_PREFIX + medId);
                if (medJson != null && !medJson.isEmpty()) {
                    try {
                        medicines.add(gson.fromJson(medJson, Medicine.class));
                    } catch (JsonSyntaxException ignored) {}
                }
            }
        }

        return gson.toJson(medicines);
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String getPrescriptionsByPatient(Context ctx, String patientId) {
        ChaincodeStub stub = ctx.getStub();
        List<Prescription> prescriptions = new ArrayList<>();

        try (QueryResultsIterator<KeyValue> results =
                     stub.getStateByRange(PRESC_PREFIX, PRESC_PREFIX + "\uFFFF")) {
            for (KeyValue kv : results) {
                Prescription p = gson.fromJson(kv.getStringValue(), Prescription.class);
                if (p != null && patientId.equals(p.getPatientId())) {
                    prescriptions.add(p);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error fetching prescriptions by patient: " + e.getMessage());
        }

        return gson.toJson(prescriptions);
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String getPrescriptionsByDoctor(Context ctx, String doctorId) {
        ChaincodeStub stub = ctx.getStub();
        List<Prescription> prescriptions = new ArrayList<>();

        try (QueryResultsIterator<KeyValue> results =
                     stub.getStateByRange(PRESC_PREFIX, PRESC_PREFIX + "\uFFFF")) {
            for (KeyValue kv : results) {
                Prescription p = gson.fromJson(kv.getStringValue(), Prescription.class);
                if (p != null && doctorId.equals(p.getDoctorId())) {
                    prescriptions.add(p);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error fetching prescriptions by doctor: " + e.getMessage());
        }

        return gson.toJson(prescriptions);
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String searchPrescriptions(Context ctx, String keyword) {
        ChaincodeStub stub = ctx.getStub();
        List<Prescription> resultsList = new ArrayList<>();

        try (QueryResultsIterator<KeyValue> results =
                     stub.getStateByRange(PRESC_PREFIX, PRESC_PREFIX + "\uFFFF")) {
            for (KeyValue kv : results) {
                Prescription p = gson.fromJson(kv.getStringValue(), Prescription.class);
                if (p != null && gson.toJson(p).toLowerCase().contains(keyword.toLowerCase())) {
                    resultsList.add(p);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error searching prescriptions: " + e.getMessage());
        }

        return gson.toJson(resultsList);
    }
}
