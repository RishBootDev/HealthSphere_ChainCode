package org.rishbootdev.chaincode.contracts;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Default;
import org.hyperledger.fabric.contract.annotation.Info;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;
import org.rishbootdev.chaincode.model.Patient;
import org.rishbootdev.chaincode.model.Prescription;
import org.rishbootdev.chaincode.model.Record;

import java.util.ArrayList;
import java.util.List;

@Contract(
        name = "RecordContract",
        info = @Info(
                title = "Record Contract",
                description = "Manages records",
                version = "1.0.0"
        )
)
@Default
public class RecordContract {

    private final Gson gson = new Gson();
    private static final String RECORD_PREFIX = "RECORD_";
    private static final String PATIENT_PREFIX = "PATIENT_";
    private static final String PRESCRIPTION_PREFIX = "PRESC_";

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public String createPatientRecord(Context ctx, String recordJson) {
        ChaincodeStub stub = ctx.getStub();
        org.rishbootdev.chaincode.model.Record record = gson.fromJson(recordJson,  org.rishbootdev.chaincode.model.Record.class);

        if (record.getRecordId() == null || record.getRecordId().isEmpty())
            throw new ChaincodeException("Record ID cannot be empty");
        if (record.getPatientId() == null || record.getPatientId().isEmpty())
            throw new ChaincodeException("Record must be linked to a Patient");

        String recordKey = RECORD_PREFIX + record.getRecordId();
        if (!stub.getStringState(recordKey).isEmpty())
            throw new ChaincodeException("Record already exists: " + record.getRecordId());

        String patientKey = PATIENT_PREFIX + record.getPatientId();
        String patientJson = stub.getStringState(patientKey);
        if (patientJson == null || patientJson.isEmpty())
            throw new ChaincodeException("Referenced Patient not found: " + record.getPatientId());

        Patient patient = gson.fromJson(patientJson, Patient.class);
        List<String> recordIds = patient.getRecordIds();
        if (recordIds == null) recordIds = new ArrayList<>();
        if (!recordIds.contains(record.getRecordId())) recordIds.add(record.getRecordId());
        patient.setRecordIds(recordIds);

        stub.putStringState(patientKey, gson.toJson(patient));
        stub.putStringState(recordKey, gson.toJson(record));
        return "Record created and linked successfully for patient: " + record.getPatientId();
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public String updatePatientRecord(Context ctx, String recordId, String recordJson) {
        ChaincodeStub stub = ctx.getStub();
        String key = RECORD_PREFIX + recordId;
        String existing = stub.getStringState(key);
        if (existing == null || existing.isEmpty())
            throw new ChaincodeException("Record not found: " + recordId);

        Record updated = gson.fromJson(recordJson, Record.class);
        if (updated.getRecordId() == null || updated.getRecordId().isEmpty())
            updated.setRecordId(recordId);

        stub.putStringState(key, gson.toJson(updated));
        return "Record updated successfully: " + recordId;
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public String deletePatientRecord(Context ctx, String recordId) {
        ChaincodeStub stub = ctx.getStub();
        String key = RECORD_PREFIX + recordId;
        String existing = stub.getStringState(key);
        if (existing == null || existing.isEmpty())
            throw new ChaincodeException("Record not found: " + recordId);

        org.rishbootdev.chaincode.model.Record record = gson.fromJson(existing,  org.rishbootdev.chaincode.model.Record.class);
        String patientId = record.getPatientId();
        String patientKey = PATIENT_PREFIX + patientId;
        String patientJson = stub.getStringState(patientKey);

        if (patientJson != null && !patientJson.isEmpty()) {
            Patient patient = gson.fromJson(patientJson, Patient.class);
            if (patient.getRecordIds() != null && patient.getRecordIds().remove(recordId))
                stub.putStringState(patientKey, gson.toJson(patient));
        }
        stub.delState(key);
        return "Record deleted and unlinked from patient: " + recordId;
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String getAllRecords(Context ctx) {
        ChaincodeStub stub = ctx.getStub();
        List<org.rishbootdev.chaincode.model.Record> records = new ArrayList<>();
        try (QueryResultsIterator<KeyValue> results =
                     stub.getStateByRange(RECORD_PREFIX, RECORD_PREFIX + "\uFFFF")) {
            for (KeyValue kv : results) {
                try {
                    org.rishbootdev.chaincode.model.Record record = gson.fromJson(kv.getStringValue(),  org.rishbootdev.chaincode.model.Record.class);
                    if (record != null && record.getRecordId() != null && !record.getRecordId().isEmpty())
                        records.add(record);
                } catch (JsonSyntaxException ignored) {}
            }
        } catch (Exception e) {
            throw new ChaincodeException("Error fetching records: " + e.getMessage());
        }
        return gson.toJson(records);
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
                    if (patient != null && patient.getPatientId() != null && !patient.getPatientId().isEmpty())
                        patients.add(patient);
                } catch (JsonSyntaxException ignored) {}
            }
        } catch (Exception e) {
            throw new ChaincodeException("Error fetching patients: " + e.getMessage());
        }
        return gson.toJson(patients);
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String searchRecords(Context ctx, String keyword) {
        ChaincodeStub stub = ctx.getStub();
        List< org.rishbootdev.chaincode.model.Record> matched = new ArrayList<>();
        try (QueryResultsIterator<KeyValue> results =
                     stub.getStateByRange(RECORD_PREFIX, RECORD_PREFIX + "\uFFFF")) {
            for (KeyValue kv : results) {
                Record record = gson.fromJson(kv.getStringValue(),  org.rishbootdev.chaincode.model.Record.class);
                if (record != null) {
                    String data = gson.toJson(record).toLowerCase();
                    if (data.contains(keyword.toLowerCase())) matched.add(record);
                }
            }
        } catch (Exception e) {
            throw new ChaincodeException("Error searching records: " + e.getMessage());
        }
        return gson.toJson(matched);
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String getPrescriptionsByPatient(Context ctx, String patientId) {
        ChaincodeStub stub = ctx.getStub();
        List<Prescription> prescriptions = new ArrayList<>();
        try (QueryResultsIterator<KeyValue> results =
                     stub.getStateByRange(PRESCRIPTION_PREFIX, PRESCRIPTION_PREFIX + "\uFFFF")) {
            for (KeyValue kv : results) {
                try {
                    Prescription pres = gson.fromJson(kv.getStringValue(), Prescription.class);
                    if (pres != null && patientId.equals(pres.getPatientId())) prescriptions.add(pres);
                } catch (JsonSyntaxException ignored) {}
            }
        } catch (Exception e) {
            throw new ChaincodeException("Error fetching prescriptions: " + e.getMessage());
        }
        return gson.toJson(prescriptions);
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public String uploadPrescription(Context ctx, String prescriptionJson) {
        ChaincodeStub stub = ctx.getStub();
        Prescription pres = gson.fromJson(prescriptionJson, Prescription.class);
        if (pres.getPrescriptionId() == null || pres.getPrescriptionId().isEmpty())
            throw new ChaincodeException("Prescription ID cannot be empty");
        if (pres.getPatientId() == null || pres.getPatientId().isEmpty())
            throw new ChaincodeException("Prescription must be linked to a valid Patient");

        String patientKey = PATIENT_PREFIX + pres.getPatientId();
        String patientJson = stub.getStringState(patientKey);
        if (patientJson == null || patientJson.isEmpty())
            throw new ChaincodeException("Referenced Patient not found: " + pres.getPatientId());

        Patient patient = gson.fromJson(patientJson, Patient.class);
        List<String> prescriptionIds = patient.getPrescriptionsIds();
        if (prescriptionIds == null) prescriptionIds = new ArrayList<>();
        if (!prescriptionIds.contains(pres.getPrescriptionId())) prescriptionIds.add(pres.getPrescriptionId());
        patient.setPrescriptionsIds(prescriptionIds);

        stub.putStringState(patientKey, gson.toJson(patient));
        String presKey = PRESCRIPTION_PREFIX + pres.getPrescriptionId();
        stub.putStringState(presKey, gson.toJson(pres));
        return "Prescription uploaded and linked to patient: " + pres.getPatientId();
    }
}
