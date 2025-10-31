package org.rishbootdev.chaincode.contracts;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.rishbootdev.chaincode.model.Patient;
import org.rishbootdev.chaincode.model.LabReport;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;
import org.hyperledger.fabric.shim.ledger.KeyValue;

import java.util.ArrayList;
import java.util.List;

@Contract(name = "PatientContract")
public class PatientContract {

    private final Gson gson = new Gson();

    private static final String PATIENT_PREFIX = "PATIENT_";
    private static final String REPORT_PREFIX = "REPORT_";


    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void createPatient(Context ctx, String patientJson) {
        ChaincodeStub stub = ctx.getStub();
        Patient patient = gson.fromJson(patientJson, Patient.class);

        if (patient.getPatientId() == null || patient.getPatientId().isEmpty()) {
            throw new RuntimeException("Patient ID cannot be empty");
        }

        String key = PATIENT_PREFIX + patient.getPatientId();
        if (!stub.getStringState(key).isEmpty()) {
            throw new RuntimeException("Patient already exists: " + patient.getPatientId());
        }

        stub.putStringState(key, gson.toJson(patient));
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String getPatient(Context ctx, String patientId) {
        String key = PATIENT_PREFIX + patientId;
        String state = ctx.getStub().getStringState(key);

        if (state == null || state.isEmpty()) {
            throw new RuntimeException("Patient not found: " + patientId);
        }
        return state;
    }


    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void updatePatient(Context ctx, String patientJson) {
        ChaincodeStub stub = ctx.getStub();
        Patient patient = gson.fromJson(patientJson, Patient.class);

        String key = PATIENT_PREFIX + patient.getPatientId();
        if (stub.getStringState(key).isEmpty()) {
            throw new RuntimeException("Patient not found: " + patient.getPatientId());
        }

        stub.putStringState(key, gson.toJson(patient));
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void deletePatient(Context ctx, String patientId) {
        ChaincodeStub stub = ctx.getStub();
        String key = PATIENT_PREFIX + patientId;

        if (stub.getStringState(key).isEmpty()) {
            throw new RuntimeException("Patient not found: " + patientId);
        }

        try (QueryResultsIterator<KeyValue> results =
                     stub.getStateByRange(REPORT_PREFIX, REPORT_PREFIX + "\uFFFF")) {
            for (KeyValue kv : results) {
                LabReport report = gson.fromJson(kv.getStringValue(), LabReport.class);
                if (report != null && patientId.equals(report.getPatientId())) {
                    stub.delState(kv.getKey());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error while deleting patient reports: " + e.getMessage());
        }

        stub.delState(key);
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String getAllPatients(Context ctx) {
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
                } catch (JsonSyntaxException ignored) {}
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return gson.toJson(patients);
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String getReportsByPatient(Context ctx, String patientId) {
        ChaincodeStub stub = ctx.getStub();
        List<LabReport> reports = new ArrayList<>();

        try (QueryResultsIterator<KeyValue> results =
                     stub.getStateByRange(REPORT_PREFIX, REPORT_PREFIX + "\uFFFF")) {
            for (KeyValue kv : results) {
                try {
                    LabReport report = gson.fromJson(kv.getStringValue(), LabReport.class);
                    if (report != null && patientId.equals(report.getPatientId())) {
                        reports.add(report);
                    }
                } catch (JsonSyntaxException ignored) {}
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return gson.toJson(reports);
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void linkReportToPatient(Context ctx, String patientId, String reportId) {
        ChaincodeStub stub = ctx.getStub();

        String patientKey = PATIENT_PREFIX + patientId;
        String patientJson = stub.getStringState(patientKey);
        if (patientJson == null || patientJson.isEmpty()) {
            throw new RuntimeException("Patient not found: " + patientId);
        }

        Patient patient = gson.fromJson(patientJson, Patient.class);
        patient.setLabReportId(reportId);
        stub.putStringState(patientKey, gson.toJson(patient));
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void unlinkReportFromPatient(Context ctx, String patientId) {
        ChaincodeStub stub = ctx.getStub();

        String patientKey = PATIENT_PREFIX + patientId;
        String patientJson = stub.getStringState(patientKey);
        if (patientJson == null || patientJson.isEmpty()) {
            throw new RuntimeException("Patient not found: " + patientId);
        }

        Patient patient = gson.fromJson(patientJson, Patient.class);
        patient.setLabReportId(null);
        stub.putStringState(patientKey, gson.toJson(patient));
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String getPatientDetails(Context ctx, String patientId) {
        return getPatient(ctx, patientId);
    }
}
