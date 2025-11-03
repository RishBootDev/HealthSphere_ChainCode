package org.rishbootdev.chaincode.contracts;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.annotation.*;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;
import org.rishbootdev.chaincode.model.*;

import java.util.ArrayList;
import java.util.List;

@Contract(
        name = "LabContract",
        info = @Info(
                title = "Lab Contract",
                description = "Handles Lab operations and relationships with Hospitals, Patients, and Lab Reports",
                version = "1.0.1"
        )
)
@Default
public class LabContract {

    private final Gson gson = new Gson();

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public Lab createLab(Context ctx, String labId, String name, String hospitalId) {
        ChaincodeStub stub = ctx.getStub();
        String labKey = "LAB_" + labId;
        if (!stub.getStringState(labKey).isEmpty()) {
            throw new ChaincodeException("Lab already exists with ID: " + labId);
        }
        Lab lab = new Lab(labId, name,hospitalId, new ArrayList<>());
        stub.putStringState(labKey, gson.toJson(lab));
        return lab;
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public Lab readLab(Context ctx, String labId) {
        String json = ctx.getStub().getStringState("LAB_" + labId);
        if (json == null || json.isEmpty()) {
            throw new ChaincodeException("Lab not found: " + labId);
        }
        return gson.fromJson(json, Lab.class);
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public Lab updateLab(Context ctx, String labId, String name) {
        Lab lab = readLab(ctx, labId);
        lab.setName(name);
        ctx.getStub().putStringState("LAB_" + labId, gson.toJson(lab));
        return lab;
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public String deleteLab(Context ctx, String labId) {
        ChaincodeStub stub = ctx.getStub();
        Lab lab = readLab(ctx, labId);
        if (lab.getReportIds() != null) {
            for (String reportId : lab.getReportIds()) {
                stub.delState("REPORT_" + reportId);
            }
        }
        stub.delState("LAB_" + labId);
        return "Deleted Lab with ID: " + labId;
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public List<Lab> getAllLabs(Context ctx) {
        ChaincodeStub stub = ctx.getStub();
        List<Lab> labs = new ArrayList<>();
        try (QueryResultsIterator<KeyValue> results = stub.getStateByRange("LAB_", "LAB_~")) {
            for (KeyValue kv : results) {
                try {
                    Lab lab = gson.fromJson(kv.getStringValue(), Lab.class);
                    if (lab != null && lab.getLabId() != null) {
                        labs.add(lab);
                    }
                } catch (JsonSyntaxException ignored) {}
            }
        } catch (Exception e) {
            throw new ChaincodeException(e.getMessage());
        }
        return labs;
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public LabReport createLabReport(Context ctx, String reportId, String patientId,
                                     String testType, String testResult, String labId,
                                     String testDate, String remarks) {
        ChaincodeStub stub = ctx.getStub();
        String reportKey = "REPORT_" + reportId;
        if (!stub.getStringState(reportKey).isEmpty()) {
            throw new ChaincodeException("Lab Report already exists with ID: " + reportId);
        }
        Lab lab = readLab(ctx, labId);
        LabReport report = new LabReport(reportId, patientId, testType, testResult, labId, testDate, remarks);
        if (!lab.getReportIds().contains(reportId)) {
            lab.getReportIds().add(reportId);
        }
        stub.putStringState("LAB_" + labId, gson.toJson(lab));
        stub.putStringState(reportKey, gson.toJson(report));

        String patientJSON = stub.getStringState("PATIENT_" + patientId);
        if (patientJSON != null && !patientJSON.isEmpty()) {
            Patient patient = gson.fromJson(patientJSON, Patient.class);
            patient.setLabReportId(reportId);
            stub.putStringState("PATIENT_" + patientId, gson.toJson(patient));
        }

        return report;
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public LabReport readLabReport(Context ctx, String reportId) {
        String json = ctx.getStub().getStringState("REPORT_" + reportId);
        if (json == null || json.isEmpty()) {
            throw new ChaincodeException("Report not found: " + reportId);
        }
        return gson.fromJson(json, LabReport.class);
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public LabReport updateLabReport(Context ctx, String reportId, String testType,
                                     String testResult, String testDate, String remarks) {
        LabReport report = readLabReport(ctx, reportId);
        report.setTestType(testType);
        report.setTestResult(testResult);
        report.setTestDate(testDate);
        report.setRemarks(remarks);
        ctx.getStub().putStringState("REPORT_" + reportId, gson.toJson(report));
        return report;
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public String deleteLabReport(Context ctx, String reportId) {
        ChaincodeStub stub = ctx.getStub();
        LabReport report = readLabReport(ctx, reportId);
        String labId = report.getLabId();
        String labJSON = stub.getStringState("LAB_" + labId);
        if (labJSON != null && !labJSON.isEmpty()) {
            Lab lab = gson.fromJson(labJSON, Lab.class);
            lab.getReportIds().remove(reportId);
            stub.putStringState("LAB_" + labId, gson.toJson(lab));
        }
        stub.delState("REPORT_" + reportId);
        return "Deleted report with ID: " + reportId;
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public List<LabReport> getAllLabReports(Context ctx) {
        ChaincodeStub stub = ctx.getStub();
        List<LabReport> reports = new ArrayList<>();
        try (QueryResultsIterator<KeyValue> results = stub.getStateByRange("REPORT_", "REPORT_~")) {
            for (KeyValue kv : results) {
                try {
                    LabReport report = gson.fromJson(kv.getStringValue(), LabReport.class);
                    if (report != null && report.getReportId() != null) {
                        reports.add(report);
                    }
                } catch (JsonSyntaxException ignored) {}
            }
        } catch (Exception e) {
            throw new ChaincodeException(e.getMessage());
        }
        return reports;
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public Lab addReportToLab(Context ctx, String labId, String reportId) {
        ChaincodeStub stub = ctx.getStub();
        Lab lab = readLab(ctx, labId);
        LabReport report = readLabReport(ctx, reportId);
        if (!lab.getReportIds().contains(reportId)) {
            lab.getReportIds().add(reportId);
        }
        report.setLabId(labId);
        stub.putStringState("LAB_" + labId, gson.toJson(lab));
        stub.putStringState("REPORT_" + reportId, gson.toJson(report));
        return lab;
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public String addLabToHospital(Context ctx, String hospitalId, String labId) {
        ChaincodeStub stub = ctx.getStub();
        String hospitalJSON = stub.getStringState("HOSPITAL_" + hospitalId);
        if (hospitalJSON == null || hospitalJSON.isEmpty()) {
            throw new ChaincodeException("Hospital not found: " + hospitalId);
        }
        Hospital hospital = gson.fromJson(hospitalJSON, Hospital.class);
        Lab lab = readLab(ctx, labId);
        if (!hospital.getLabIds().contains(labId)) {
            hospital.getLabIds().add(labId);
        }
        lab.setHospitalId(hospitalId);
        stub.putStringState("HOSPITAL_" + hospitalId, gson.toJson(hospital));
        stub.putStringState("LAB_" + labId, gson.toJson(lab));
        return "Added Lab " + labId + " to Hospital " + hospitalId;
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public List<LabReport> getReportsByPatient(Context ctx, String patientId) {
        ChaincodeStub stub = ctx.getStub();
        List<LabReport> reports = new ArrayList<>();
        try (QueryResultsIterator<KeyValue> results = stub.getStateByRange("REPORT_", "REPORT_~")) {
            for (KeyValue kv : results) {
                try {
                    LabReport report = gson.fromJson(kv.getStringValue(), LabReport.class);
                    if (report != null && patientId.equals(report.getPatientId())) {
                        reports.add(report);
                    }
                } catch (JsonSyntaxException ignored) {}
            }
        } catch (Exception e) {
            throw new ChaincodeException(e.getMessage());
        }
        return reports;
    }
}
