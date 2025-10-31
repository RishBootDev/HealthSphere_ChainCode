package org.rishbootdev.chaincode.contracts;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.rishbootdev.chaincode.model.Record;
import org.rishbootdev.chaincode.model.Patient;
import org.rishbootdev.chaincode.model.Prescription;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;

import java.util.ArrayList;
import java.util.List;

@Contract(name = "RecordContract")
public class RecordContract {

    private final Gson gson = new Gson();

    private static final String RECORD_PREFIX = "RECORD_";
    private static final String PATIENT_PREFIX = "PATIENT_";
    private static final String PRESCRIPTION_PREFIX = "PRESC_";


    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public String createPatientRecord(Context ctx, String recordJson) {
        ChaincodeStub stub = ctx.getStub();
        Record record = gson.fromJson(recordJson, Record.class);

        if (record.getRecordId() == null || record.getRecordId().isEmpty()) {
            throw new RuntimeException("Record ID cannot be empty");
        }

        String key = RECORD_PREFIX + record.getRecordId();
        if (!stub.getStringState(key).isEmpty()) {
            throw new RuntimeException("Record already exists: " + record.getRecordId());
        }

        stub.putStringState(key, gson.toJson(record));
        return " Record created successfully for patient: " + record.getPatientId();
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public String updatePatientRecord(Context ctx, String recordId, String recordJson) {
        ChaincodeStub stub = ctx.getStub();
        String key = RECORD_PREFIX + recordId;

        if (stub.getStringState(key).isEmpty()) {
            throw new RuntimeException("Record not found: " + recordId);
        }

        Record record = gson.fromJson(recordJson, Record.class);
        stub.putStringState(key, gson.toJson(record));
        return "Record updated: " + recordId;
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String getPatients(Context ctx) {
        ChaincodeStub stub = ctx.getStub();
        List<Patient> patients = new ArrayList<>();

        try (QueryResultsIterator<KeyValue> results =
                     stub.getStateByRange(PATIENT_PREFIX, PATIENT_PREFIX + "\uFFFF")) {
            for (KeyValue kv : results) {
                try {
                    Patient patient = gson.fromJson(kv.getStringValue(), Patient.class);
                    if (patient != null && patient.getPatientId() != null) {
                        patients.add(patient);
                    }
                } catch (JsonSyntaxException ignore) {}
            }
        } catch (Exception e) {
            throw new RuntimeException("Error fetching patients: " + e.getMessage());
        }

        return gson.toJson(patients);
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String searchRecords(Context ctx, String keyword) {
        ChaincodeStub stub = ctx.getStub();
        List<Record> records = new ArrayList<>();

        try (QueryResultsIterator<KeyValue> results =
                     stub.getStateByRange(RECORD_PREFIX, RECORD_PREFIX + "\uFFFF")) {
            for (KeyValue kv : results) {
                Record record = gson.fromJson(kv.getStringValue(), Record.class);
                if (record != null) {
                    String data = gson.toJson(record).toLowerCase();
                    if (data.contains(keyword.toLowerCase())) {
                        records.add(record);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error searching records: " + e.getMessage());
        }

        return gson.toJson(records);
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String getPrescriptionsByPatient(Context ctx, String patientId) {
        ChaincodeStub stub = ctx.getStub();
        List<Prescription> prescriptions = new ArrayList<>();

        try (QueryResultsIterator<KeyValue> results =
                     stub.getStateByRange(PRESCRIPTION_PREFIX, PRESCRIPTION_PREFIX + "\uFFFF")) {
            for (KeyValue kv : results) {
                Prescription pres = gson.fromJson(kv.getStringValue(), Prescription.class);
                if (pres != null && patientId.equals(pres.getPatientId())) {
                    prescriptions.add(pres);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error fetching prescriptions: " + e.getMessage());
        }

        return gson.toJson(prescriptions);
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public String uploadPrescription(Context ctx, String prescriptionJson) {
        ChaincodeStub stub = ctx.getStub();
        Prescription pres = gson.fromJson(prescriptionJson, Prescription.class);

        if (pres.getPrescriptionId() == null || pres.getPrescriptionId().isEmpty()) {
            throw new RuntimeException("Prescription ID cannot be empty");
        }

        String key = PRESCRIPTION_PREFIX + pres.getPrescriptionId();
        stub.putStringState(key, gson.toJson(pres));

        return "Prescription uploaded successfully for patient: " + pres.getPatientId();
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String getAllRecords(Context ctx) {
        ChaincodeStub stub = ctx.getStub();
        List<Record> records = new ArrayList<>();

        try (QueryResultsIterator<KeyValue> results =
                     stub.getStateByRange(RECORD_PREFIX, RECORD_PREFIX + "\uFFFF")) {
            for (KeyValue kv : results) {
                try {
                    Record record = gson.fromJson(kv.getStringValue(), Record.class);
                    if (record != null && record.getRecordId() != null) {
                        records.add(record);
                    }
                } catch (JsonSyntaxException ignore) {}
            }
        } catch (Exception e) {
            throw new RuntimeException("Error fetching records: " + e.getMessage());
        }

        return gson.toJson(records);
    }
}
