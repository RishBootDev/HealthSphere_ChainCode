package org.rishbootdev.chaincode.contracts;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Default;
import org.hyperledger.fabric.contract.annotation.Info;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;
import org.rishbootdev.chaincode.model.Patient;
import org.rishbootdev.chaincode.model.LabReport;
import org.rishbootdev.chaincode.model.Doctor;
import org.rishbootdev.chaincode.model.Hospital;

import java.util.ArrayList;
import java.util.List;

@Contract(
        name = "PatientContract",
        info = @Info(
                title = "PatientContract",
                description = "Manages relationships between Patients and all other operations performed on or by the patient",
                version = "1.0.0"
        )
)
@Default
public class PatientContract implements ContractInterface {

    private final Gson gson = new Gson();

    private static final String PATIENT_PREFIX = "PATIENT_";
    private static final String REPORT_PREFIX = "REPORT_";
    private static final String DOCTOR_PREFIX = "DOCTOR_";
    private static final String HOSPITAL_PREFIX = "HOSPITAL_";

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void createPatient(Context ctx, String patientJson) {
        ChaincodeStub stub = ctx.getStub();
        Patient patient = gson.fromJson(patientJson, Patient.class);
        if (patient.getPatientId() == null || patient.getPatientId().isEmpty()) {
            throw new ChaincodeException("Patient ID cannot be empty");
        }
        String key = PATIENT_PREFIX + patient.getPatientId();
        if (!stub.getStringState(key).isEmpty()) {
            throw new ChaincodeException("Patient already exists: " + patient.getPatientId());
        }
        stub.putStringState(key, gson.toJson(patient));
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String getPatient(Context ctx, String patientId) {
        String key = PATIENT_PREFIX + patientId;
        String state = ctx.getStub().getStringState(key);
        if (state == null || state.isEmpty()) {
            throw new ChaincodeException("Patient not found: " + patientId);
        }
        return state;
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void updatePatient(Context ctx, String patientJson) {
        ChaincodeStub stub = ctx.getStub();
        Patient updated = gson.fromJson(patientJson, Patient.class);
        String key = PATIENT_PREFIX + updated.getPatientId();
        if (stub.getStringState(key).isEmpty()) {
            throw new ChaincodeException("Patient not found: " + updated.getPatientId());
        }
        stub.putStringState(key, gson.toJson(updated));
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void deletePatient(Context ctx, String patientId) {
        ChaincodeStub stub = ctx.getStub();
        String key = PATIENT_PREFIX + patientId;
        if (stub.getStringState(key).isEmpty()) {
            throw new ChaincodeException("Patient not found: " + patientId);
        }
        try (QueryResultsIterator<KeyValue> results = stub.getStateByRange(REPORT_PREFIX, REPORT_PREFIX + "\uFFFF")) {
            for (KeyValue kv : results) {
                LabReport report = gson.fromJson(kv.getStringValue(), LabReport.class);
                if (report != null && patientId.equals(report.getPatientId())) {
                    stub.delState(kv.getKey());
                }
            }
        } catch (Exception e) {
            throw new ChaincodeException("Error while deleting patient reports: " + e.getMessage());
        }
        stub.delState(key);
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String getAllPatients(Context ctx) {
        ChaincodeStub stub = ctx.getStub();
        List<Patient> patients = new ArrayList<>();
        try (QueryResultsIterator<KeyValue> results = stub.getStateByRange(PATIENT_PREFIX, PATIENT_PREFIX + "\uFFFF")) {
            for (KeyValue kv : results) {
                try {
                    Patient patient = gson.fromJson(kv.getStringValue(), Patient.class);
                    if (patient != null && patient.getPatientId() != null) {
                        patients.add(patient);
                    }
                } catch (JsonSyntaxException ignored) {}
            }
        } catch (Exception e) {
            throw new ChaincodeException("Error fetching all patients: " + e.getMessage());
        }
        return gson.toJson(patients);
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void assignDoctorToPatient(Context ctx, String patientId, String doctorId) {
        ChaincodeStub stub = ctx.getStub();
        String patientKey = PATIENT_PREFIX + patientId;
        String doctorKey = DOCTOR_PREFIX + doctorId;
        String patientJson = stub.getStringState(patientKey);
        String doctorJson = stub.getStringState(doctorKey);
        if (patientJson.isEmpty())
            throw new ChaincodeException("Patient not found: " + patientId);
        if (doctorJson.isEmpty())
            throw new ChaincodeException("Doctor not found: " + doctorId);

        Patient patient = gson.fromJson(patientJson, Patient.class);
        Doctor doctor = gson.fromJson(doctorJson, Doctor.class);
        patient.setDoctorId(doctorId);
        stub.putStringState(patientKey, gson.toJson(patient));
        List<String> patientIds = doctor.getPatientIds();
        if (!patientIds.contains(patientId)) {
            patientIds.add(patientId);
            doctor.setPatientIds(patientIds);
        }
        stub.putStringState(doctorKey, gson.toJson(doctor));
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void removeDoctorFromPatient(Context ctx, String patientId) {
        ChaincodeStub stub = ctx.getStub();
        String patientKey = PATIENT_PREFIX + patientId;
        String patientJson = stub.getStringState(patientKey);
        if (patientJson.isEmpty())
            throw new ChaincodeException("Patient not found: " + patientId);

        Patient patient = gson.fromJson(patientJson, Patient.class);
        String doctorId = patient.getDoctorId();
        if (doctorId != null) {
            String doctorKey = DOCTOR_PREFIX + doctorId;
            String doctorJson = stub.getStringState(doctorKey);
            if (!doctorJson.isEmpty()) {
                Doctor doctor = gson.fromJson(doctorJson, Doctor.class);
                doctor.getPatientIds().remove(patientId);
                stub.putStringState(doctorKey, gson.toJson(doctor));
            }
        }
        patient.setDoctorId(null);
        stub.putStringState(patientKey, gson.toJson(patient));
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void assignHospitalToPatient(Context ctx, String patientId, String hospitalId) {
        ChaincodeStub stub = ctx.getStub();
        String patientKey = PATIENT_PREFIX + patientId;
        String hospitalKey = HOSPITAL_PREFIX + hospitalId;
        String patientJson = stub.getStringState(patientKey);
        String hospitalJson = stub.getStringState(hospitalKey);

        if (patientJson.isEmpty())
            throw new ChaincodeException("Patient not found: " + patientId);
        if (hospitalJson.isEmpty())
            throw new ChaincodeException("Hospital not found: " + hospitalId);

        Patient patient = gson.fromJson(patientJson, Patient.class);
        Hospital hospital = gson.fromJson(hospitalJson, Hospital.class);
        patient.setHospitalId(hospitalId);
        stub.putStringState(patientKey, gson.toJson(patient));
        List<String> patientIds = hospital.getPatientIds();
        if (!patientIds.contains(patientId)) {
            patientIds.add(patientId);
            hospital.setPatientIds(patientIds);
        }
        stub.putStringState(hospitalKey, gson.toJson(hospital));
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void removeHospitalFromPatient(Context ctx, String patientId) {
        ChaincodeStub stub = ctx.getStub();
        String patientKey = PATIENT_PREFIX + patientId;
        String patientJson = stub.getStringState(patientKey);

        if (patientJson.isEmpty())
            throw new ChaincodeException("Patient not found: " + patientId);

        Patient patient = gson.fromJson(patientJson, Patient.class);

        String hospitalId = patient.getHospitalId();

        if (hospitalId != null) {
            String hospitalKey = HOSPITAL_PREFIX + hospitalId;
            String hospitalJson = stub.getStringState(hospitalKey);
            if (!hospitalJson.isEmpty()) {
                Hospital hospital = gson.fromJson(hospitalJson, Hospital.class);
                hospital.getPatientIds().remove(patientId);
                stub.putStringState(hospitalKey, gson.toJson(hospital));
            }
        }
        patient.setHospitalId(null);
        stub.putStringState(patientKey, gson.toJson(patient));
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void linkReportToPatient(Context ctx, String patientId, String reportId) {
        ChaincodeStub stub = ctx.getStub();
        String patientKey = PATIENT_PREFIX + patientId;
        String patientJson = stub.getStringState(patientKey);

        if (patientJson.isEmpty())
            throw new ChaincodeException("Patient not found: " + patientId);

        Patient patient = gson.fromJson(patientJson, Patient.class);

        patient.setLabReportId(reportId);
        stub.putStringState(patientKey, gson.toJson(patient));
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void unlinkReportFromPatient(Context ctx, String patientId) {
        ChaincodeStub stub = ctx.getStub();
        String patientKey = PATIENT_PREFIX + patientId;
        String patientJson = stub.getStringState(patientKey);

        if (patientJson.isEmpty())
            throw new ChaincodeException("Patient not found: " + patientId);

        Patient patient = gson.fromJson(patientJson, Patient.class);
        patient.setLabReportId(null);
        stub.putStringState(patientKey, gson.toJson(patient));
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String getReportsByPatient(Context ctx, String patientId) {
        ChaincodeStub stub = ctx.getStub();
        List<LabReport> reports = new ArrayList<>();
        try (QueryResultsIterator<KeyValue> results = stub.getStateByRange(REPORT_PREFIX, REPORT_PREFIX + "\uFFFF")) {
            for (KeyValue kv : results) {
                LabReport report = gson.fromJson(kv.getStringValue(), LabReport.class);
                if (report != null && patientId.equals(report.getPatientId())) {
                    reports.add(report);
                }
            }
        } catch (Exception e) {
            throw new ChaincodeException("Error fetching reports: " + e.getMessage());
        }
        return gson.toJson(reports);
    }
}
