package org.rishbootdev.chaincode.contracts;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.hyperledger.fabric.contract.annotation.Default;
import org.hyperledger.fabric.contract.annotation.Info;
import org.rishbootdev.chaincode.model.Doctor;
import org.rishbootdev.chaincode.model.Patient;
import org.rishbootdev.chaincode.model.Record;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;

import java.util.ArrayList;
import java.util.List;

@Contract(
        name = "DoctorContract",
        info = @Info(
                title = "Doctor Contract",
                description = "Manages relationships between Patients",
                version = "1.0.0"
        )
)
@Default
public class DoctorContract {

    private final Gson gson = new Gson();
    private static final String DOCTOR_PREFIX = "DOCTOR_";
    private static final String PATIENT_PREFIX = "PATIENT_";
    private static final String RECORD_PREFIX = "RECORD_";

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void createDoctor(Context ctx, String doctorJson) {
        ChaincodeStub stub = ctx.getStub();
        Doctor doctor = gson.fromJson(doctorJson, Doctor.class);

        if (doctor.getDoctorId() == null || doctor.getDoctorId().isEmpty()) {
            throw new RuntimeException("Doctor ID cannot be empty");
        }

        String key = DOCTOR_PREFIX + doctor.getDoctorId();
        if (!stub.getStringState(key).isEmpty()) {
            throw new RuntimeException("Doctor already exists: " + doctor.getDoctorId());
        }

        if (doctor.getPatientIds() == null) doctor.setPatientIds(new ArrayList<>());
        if (doctor.getRecordIds() == null) doctor.setRecordIds(new ArrayList<>());

        stub.putStringState(key, gson.toJson(doctor));
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String getDoctorById(Context ctx, String doctorId) {
        String state = ctx.getStub().getStringState(DOCTOR_PREFIX + doctorId);
        if (state == null || state.isEmpty()) {
            throw new RuntimeException("Doctor not found: " + doctorId);
        }
        return state;
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public String updateDoctor(Context ctx, String doctorJson) {
        ChaincodeStub stub = ctx.getStub();
        Doctor doctor = gson.fromJson(doctorJson, Doctor.class);

        String key = DOCTOR_PREFIX + doctor.getDoctorId();
        if (stub.getStringState(key).isEmpty()) {
            throw new RuntimeException("Doctor not found: " + doctor.getDoctorId());
        }

        if (doctor.getPatientIds() == null) doctor.setPatientIds(new ArrayList<>());
        if (doctor.getRecordIds() == null) doctor.setRecordIds(new ArrayList<>());

        stub.putStringState(key, gson.toJson(doctor));
        return "Doctor updated: " + doctor.getDoctorId();
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public String deleteDoctor(Context ctx, String doctorId) {
        ChaincodeStub stub = ctx.getStub();
        String key = DOCTOR_PREFIX + doctorId;

        if (stub.getStringState(key).isEmpty()) {
            throw new RuntimeException("Doctor not found: " + doctorId);
        }

        stub.delState(key);
        return "Doctor deleted successfully: " + doctorId;
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String getAllDoctors(Context ctx) {
        ChaincodeStub stub = ctx.getStub();
        List<Doctor> doctors = new ArrayList<>();

        try (QueryResultsIterator<KeyValue> results = stub.getStateByRange("", "")) {
            for (KeyValue kv : results) {
                if (kv.getKey().startsWith(DOCTOR_PREFIX)) {
                    try {
                        Doctor doc = gson.fromJson(kv.getStringValue(), Doctor.class);
                        if (doc != null && doc.getDoctorId() != null) {
                            doctors.add(doc);
                        }
                    } catch (JsonSyntaxException ignore) {}
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error fetching doctors: " + e.getMessage());
        }
        return gson.toJson(doctors);
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String GetRecordsByDoctor(Context ctx, String doctorId) {
        ChaincodeStub stub = ctx.getStub();
        List<Record> records = new ArrayList<>();

        try (QueryResultsIterator<KeyValue> results = stub.getStateByRange("", "")) {
            for (KeyValue kv : results) {
                if (kv.getKey().startsWith(RECORD_PREFIX)) {
                    try {
                        Record record = gson.fromJson(kv.getStringValue(), Record.class);
                        if (record != null && doctorId.equals(record.getDoctorId())) {
                            records.add(record);
                        }
                    } catch (JsonSyntaxException ignore) {}
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error fetching records: " + e.getMessage());
        }
        return gson.toJson(records);
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String GetPatientsByDoctor(Context ctx, String doctorId) {
        ChaincodeStub stub = ctx.getStub();
        List<Patient> patients = new ArrayList<>();

        try (QueryResultsIterator<KeyValue> patientResults = stub.getStateByRange("", "")) {
            for (KeyValue kv : patientResults) {
                if (kv.getKey().startsWith(PATIENT_PREFIX)) {
                    try {
                        Patient patient = gson.fromJson(kv.getStringValue(), Patient.class);
                        if (patient != null && doctorId.equals(patient.getDoctorId())) {
                            patients.add(patient);
                        }
                    } catch (JsonSyntaxException ignore) {}
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving patients: " + e.getMessage());
        }

        return gson.toJson(patients);
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public String RegisterDoctor(Context ctx, String doctorId, String name,
                                 String specialization, String hospitalId,
                                 String qualification,String contact) {
        ChaincodeStub stub = ctx.getStub();

        Doctor doctor = new Doctor();
        doctor.setDoctorId(doctorId);
        doctor.setName(name);
        doctor.setSpecialization(specialization);
        doctor.setHospitalId(hospitalId);
        doctor.setPatientIds(new ArrayList<>());
        doctor.setRecordIds(new ArrayList<>());
        doctor.setQualification(qualification);
        doctor.setContact(contact);

        String key = DOCTOR_PREFIX + doctorId;
        if (!stub.getStringState(key).isEmpty()) {
            throw new RuntimeException("Doctor already exists: " + doctorId);
        }

        stub.putStringState(key, gson.toJson(doctor));
        return "Doctor registered successfully: " + name;
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void addPatientToDoctor(Context ctx, String doctorId, String patientId) {
        ChaincodeStub stub = ctx.getStub();

        String doctorKey = DOCTOR_PREFIX + doctorId;
        String doctorJson = stub.getStringState(doctorKey);
        if (doctorJson == null || doctorJson.isEmpty()) {
            throw new RuntimeException("Doctor not found: " + doctorId);
        }
        Doctor doctor = gson.fromJson(doctorJson, Doctor.class);
        if (doctor.getPatientIds() == null) doctor.setPatientIds(new ArrayList<>());
        if (!doctor.getPatientIds().contains(patientId)) {
            doctor.getPatientIds().add(patientId);
            stub.putStringState(doctorKey, gson.toJson(doctor));
        }

        String patientKey = PATIENT_PREFIX + patientId;
        String patientJson = stub.getStringState(patientKey);
        if (patientJson == null || patientJson.isEmpty()) {
            throw new RuntimeException("Patient not found: " + patientId);
        }
        Patient patient = gson.fromJson(patientJson, Patient.class);
        patient.setDoctorId(doctorId);
        stub.putStringState(patientKey, gson.toJson(patient));
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void removePatientFromDoctor(Context ctx, String doctorId, String patientId) {
        ChaincodeStub stub = ctx.getStub();

        String doctorKey = DOCTOR_PREFIX + doctorId;
        String doctorJson = stub.getStringState(doctorKey);
        if (doctorJson == null || doctorJson.isEmpty()) {
            throw new RuntimeException("Doctor not found: " + doctorId);
        }
        Doctor doctor = gson.fromJson(doctorJson, Doctor.class);
        if (doctor.getPatientIds() != null && doctor.getPatientIds().remove(patientId)) {
            stub.putStringState(doctorKey, gson.toJson(doctor));
        }

        String patientKey = PATIENT_PREFIX + patientId;
        String patientJson = stub.getStringState(patientKey);
        if (patientJson != null && !patientJson.isEmpty()) {
            Patient patient = gson.fromJson(patientJson, Patient.class);
            if (doctorId.equals(patient.getDoctorId())) {
                patient.setDoctorId(null);
                stub.putStringState(patientKey, gson.toJson(patient));
            }
        }
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void addRecordToDoctor(Context ctx, String doctorId, String recordId) {
        ChaincodeStub stub = ctx.getStub();

        String doctorKey = DOCTOR_PREFIX + doctorId;
        String doctorJson = stub.getStringState(doctorKey);
        if (doctorJson == null || doctorJson.isEmpty()) {
            throw new RuntimeException("Doctor not found: " + doctorId);
        }
        Doctor doctor = gson.fromJson(doctorJson, Doctor.class);
        if (doctor.getRecordIds() == null) doctor.setRecordIds(new ArrayList<>());
        if (!doctor.getRecordIds().contains(recordId)) {
            doctor.getRecordIds().add(recordId);
            stub.putStringState(doctorKey, gson.toJson(doctor));
        }
    }
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void removeRecordFromDoctor(Context ctx, String doctorId, String recordId) {
        ChaincodeStub stub = ctx.getStub();

        String doctorKey = DOCTOR_PREFIX + doctorId;
        String doctorJson = stub.getStringState(doctorKey);
        if (doctorJson == null || doctorJson.isEmpty()) {
            throw new RuntimeException("Doctor not found: " + doctorId);
        }
        Doctor doctor = gson.fromJson(doctorJson, Doctor.class);
        if (doctor.getRecordIds() != null && doctor.getRecordIds().remove(recordId)) {
            stub.putStringState(doctorKey, gson.toJson(doctor));
        }
    }


    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String getAllDoctorsFast(Context ctx) {
        ChaincodeStub stub = ctx.getStub();
        List<Doctor> doctors = new ArrayList<>();

        try (QueryResultsIterator<KeyValue> results = stub.getStateByRange(DOCTOR_PREFIX, DOCTOR_PREFIX + "~")) {
            for (KeyValue kv : results) {
                try {
                    Doctor doc = gson.fromJson(kv.getStringValue(), Doctor.class);
                    if (doc != null && doc.getDoctorId() != null) {
                        doctors.add(doc);
                    }
                } catch (JsonSyntaxException ignore) {}
            }
        } catch (Exception e) {
            throw new RuntimeException("Error fetching doctors: " + e.getMessage());
        }

        return gson.toJson(doctors);
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String GetRecordsByDoctorFast(Context ctx, String doctorId) {
        ChaincodeStub stub = ctx.getStub();
        List<Record> records = new ArrayList<>();

        try (QueryResultsIterator<KeyValue> results = stub.getStateByRange(RECORD_PREFIX, RECORD_PREFIX + "~")) {
            for (KeyValue kv : results) {
                try {
                    Record record = gson.fromJson(kv.getStringValue(), Record.class);
                    if (record != null && doctorId.equals(record.getDoctorId())) {
                        records.add(record);
                    }
                } catch (JsonSyntaxException ignore) {}
            }
        } catch (Exception e) {
            throw new RuntimeException("Error fetching records: " + e.getMessage());
        }

        return gson.toJson(records);
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String GetPatientsByDoctorFast(Context ctx, String doctorId) {
        ChaincodeStub stub = ctx.getStub();
        List<Patient> patients = new ArrayList<>();

        try (QueryResultsIterator<KeyValue> results = stub.getStateByRange(PATIENT_PREFIX, PATIENT_PREFIX + "~")) {
            for (KeyValue kv : results) {
                try {
                    Patient patient = gson.fromJson(kv.getStringValue(), Patient.class);
                    if (patient != null && doctorId.equals(patient.getDoctorId())) {
                        patients.add(patient);
                    }
                } catch (JsonSyntaxException ignore) {}
            }
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving patients: " + e.getMessage());
        }

        return gson.toJson(patients);
    }



}
