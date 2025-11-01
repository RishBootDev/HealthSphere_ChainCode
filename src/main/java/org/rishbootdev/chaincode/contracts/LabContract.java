package org.rishbootdev.chaincode.contracts;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.rishbootdev.chaincode.model.Lab;
import org.rishbootdev.chaincode.model.LabReport;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;

import java.util.ArrayList;
import java.util.List;

//import static org.HimanshuTech.chaincode.contracts.PatientContract.REPORT_PREFIX;

@Contract(name = "LabContract")
public class LabContract {

    private final Gson gson = new Gson();

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void createLab(Context ctx, String labJson) {
        ChaincodeStub stub = ctx.getStub();
        Lab lab = gson.fromJson(labJson, Lab.class);

        if (lab.getLabId() == null || lab.getLabId().isEmpty()) {
            throw new RuntimeException("Lab ID cannot be empty");
        }

        String key = "LAB_" + lab.getLabId();
        if (!stub.getStringState(key).isEmpty()) {
            throw new RuntimeException("Lab already exists: " + lab.getLabId());
        }

        if (lab.getReportIds() == null) {
            lab.setReportIds(new ArrayList<>());
        }

        stub.putStringState(key, gson.toJson(lab));
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String getLab(Context ctx, String labId) {
        String key = "LAB_" + labId;
        String json = ctx.getStub().getStringState(key);
        if (json == null || json.isEmpty()) {
            throw new RuntimeException("Lab not found: " + labId);
        }
        return json;
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String getAllLabs(Context ctx) throws Exception {
        ChaincodeStub stub = ctx.getStub();
        List<Lab> labs = new ArrayList<>();

        String startKey = "LAB_";
        String endKey = "LAB_\uFFFF";

        try (QueryResultsIterator<KeyValue> results = stub.getStateByRange(startKey, endKey)) {
            for (KeyValue kv : results) {
                try {
                    Lab lab = gson.fromJson(kv.getStringValue(), Lab.class);
                    if (lab != null && lab.getLabId() != null) {
                        labs.add(lab);
                    }
                } catch (JsonSyntaxException e) {
                    System.out.println("Skipping invalid Lab record");
                }
            }
        }

        return gson.toJson(labs);
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void updateLab(Context ctx, String labJson) {
        ChaincodeStub stub = ctx.getStub();
        Lab lab = gson.fromJson(labJson, Lab.class);

        String key = "LAB_" + lab.getLabId();
        if (stub.getStringState(key).isEmpty()) {
            throw new RuntimeException("Lab not found: " + lab.getLabId());
        }

        stub.putStringState(key, gson.toJson(lab));
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void deleteLab(Context ctx, String labId) {
        ChaincodeStub stub = ctx.getStub();
        String key = "LAB_" + labId;

        String json = stub.getStringState(key);
        if (json == null || json.isEmpty()) {
            throw new RuntimeException("Lab not found: " + labId);
        }

        Lab lab = gson.fromJson(json, Lab.class);
        if (lab.getReportIds() != null) {
            for (String reportId : lab.getReportIds()) {
                stub.delState("REPORT_" + reportId);
            }
        }

        stub.delState(key);
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void createLabReport(Context ctx, String reportJson) {
        ChaincodeStub stub = ctx.getStub();
        LabReport report = gson.fromJson(reportJson, LabReport.class);

        if (report.getReportId() == null || report.getReportId().isEmpty()) {
            throw new RuntimeException("Report ID cannot be empty");
        }

        String key = "REPORT_" + report.getReportId();
        if (!stub.getStringState(key).isEmpty()) {
            throw new RuntimeException("Lab report already exists: " + report.getReportId());
        }

        String labKey = "LAB_" + report.getLabId();
        String labJson = stub.getStringState(labKey);
        if (labJson == null || labJson.isEmpty()) {
            throw new RuntimeException("Associated Lab not found: " + report.getLabId());
        }

        stub.putStringState(key, gson.toJson(report));

        Lab lab = gson.fromJson(labJson, Lab.class);
        List<String> reportIds = lab.getReportIds();
        if (reportIds == null) {
            reportIds = new ArrayList<>();
        }
        reportIds.add(report.getReportId());
        lab.setReportIds(reportIds);
        stub.putStringState(labKey, gson.toJson(lab));
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String getLabReport(Context ctx, String reportId) {
        String key = "REPORT_" + reportId;
        String json = ctx.getStub().getStringState(key);
        if (json == null || json.isEmpty()) {
            throw new RuntimeException("Report not found: " + reportId);
        }
        return json;
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String getAllLabReports(Context ctx) {
        ChaincodeStub stub = ctx.getStub();
        List<LabReport> reports = new ArrayList<>();

        String startKey = "REPORT_";
        String endKey = "REPORT_\uFFFF";

        try (QueryResultsIterator<KeyValue> results = stub.getStateByRange(startKey, endKey)) {
            for (KeyValue kv : results) {
                try {
                    LabReport report = gson.fromJson(kv.getStringValue(), LabReport.class);
                    if (report != null && report.getReportId() != null) {
                        reports.add(report);
                    }
                } catch (JsonSyntaxException e) {
                    System.out.println("Skipping invalid report JSON");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return gson.toJson(reports);
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void updateLabReport(Context ctx, String reportJson) {
        ChaincodeStub stub = ctx.getStub();
        LabReport report = gson.fromJson(reportJson, LabReport.class);

        String key = "REPORT_" + report.getReportId();
        if (stub.getStringState(key).isEmpty()) {
            throw new RuntimeException("Report not found: " + report.getReportId());
        }

        stub.putStringState(key, gson.toJson(report));
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void deleteLabReport(Context ctx, String reportId) {
        ChaincodeStub stub = ctx.getStub();
        String key = "REPORT_" + reportId;
        String json = stub.getStringState(key);

        if (json == null || json.isEmpty()) {
            throw new RuntimeException("Report not found: " + reportId);
        }

        LabReport report = gson.fromJson(json, LabReport.class);
        String labKey = "LAB_" + report.getLabId();
        String labJson = stub.getStringState(labKey);
        if (labJson != null && !labJson.isEmpty()) {
            Lab lab = gson.fromJson(labJson, Lab.class);
            List<String> reports = lab.getReportIds();
            if (reports != null) {
                reports.remove(reportId);
                lab.setReportIds(reports);
                stub.putStringState(labKey, gson.toJson(lab));
            }
        }

        stub.delState(key);
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String getReportsByLab(Context ctx, String labId) {
        ChaincodeStub stub = ctx.getStub();
        List<LabReport> reports = new ArrayList<>();

        String startKey = "REPORT_";
        String endKey = "REPORT_\uFFFF";

        try (QueryResultsIterator<KeyValue> results = stub.getStateByRange(startKey, endKey)) {
            for (KeyValue kv : results) {
                try {
                    LabReport report = gson.fromJson(kv.getStringValue(), LabReport.class);
                    if (report != null && labId.equals(report.getLabId())) {
                        reports.add(report);
                    }
                } catch (JsonSyntaxException e) {
                    System.out.println("Skipping invalid report during relation fetch");
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return gson.toJson(reports);
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String getReportsByPatient(Context ctx, String patientId) throws Exception {
        ChaincodeStub stub = ctx.getStub();
        List<LabReport> reports = new ArrayList<>();

        String startKey = "REPORT_";
        String endKey = "REPORT_\uFFFF";

        try (QueryResultsIterator<KeyValue> results = stub.getStateByRange(startKey, endKey)) {
            for (KeyValue kv : results) {
                try {
                    LabReport report = gson.fromJson(kv.getStringValue(), LabReport.class);
                    if (report != null && patientId.equals(report.getPatientId())) {
                        reports.add(report);
                    }
                } catch (JsonSyntaxException e) {
                    System.out.println("Skipping invalid report during patient search");
                }
            }
        }

        return gson.toJson(reports);
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String getLabReportsByPatientId(Context ctx, String patientId) throws Exception {
        ChaincodeStub stub = ctx.getStub();
        Gson gson = new Gson();
        List<LabReport> reports = new ArrayList<>();

        try (QueryResultsIterator<KeyValue> results = stub.getStateByRange("", "")) {
            for (KeyValue kv : results) {
                try {
                    LabReport report = gson.fromJson(kv.getStringValue(), LabReport.class);
                    if (report != null && patientId.equals(report.getPatientId())) {
                        reports.add(report);
                    }
                } catch (JsonSyntaxException e) {
                    System.out.println("error in parsing json");
                }
            }
        }

        return gson.toJson(reports);
    }

}

