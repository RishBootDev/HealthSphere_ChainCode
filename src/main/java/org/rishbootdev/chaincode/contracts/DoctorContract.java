package org.rishbootdev.chaincode.contracts;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
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

@Contract(name = "DoctorContract")
public class DoctorContract {

    private final Gson gson = new Gson();
    private static final String DOCTOR_PREFIX = "DOCTOR_";
    private static final String PATIENT_PREFIX = "PATIENT_";
    private static final String RECORD_PREFIX = "RECORD_";

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void CreateDoctor(Context ctx, String doctorJson) {
        ChaincodeStub stub = ctx.getStub();
        Doctor doctor = gson.fromJson(doctorJson, Doctor.class);

        if (doctor.getDoctorId() == null || doctor.getDoctorId().isEmpty()) {
            throw new RuntimeException("Doctor ID cannot be empty");
        }

        String key = DOCTOR_PREFIX + doctor.getDoctorId();
        if (!stub.getStringState(key).isEmpty()) {
            throw new RuntimeException("Doctor already exists: " + doctor.getDoctorId());
        }

        stub.putStringState(key, gson.toJson(doctor));
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String GetDoctorById(Context ctx, String doctorId) {
        String state = ctx.getStub().getStringState(DOCTOR_PREFIX + doctorId);
        if (state == null || state.isEmpty()) {
            throw new RuntimeException("Doctor not found: " + doctorId);
        }
        return state;
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public String UpdateDoctor(Context ctx, String doctorJson) {
        ChaincodeStub stub = ctx.getStub();
        Doctor doctor = gson.fromJson(doctorJson, Doctor.class);

        String key = DOCTOR_PREFIX + doctor.getDoctorId();
        if (stub.getStringState(key).isEmpty()) {
            throw new RuntimeException("Doctor not found: " + doctor.getDoctorId());
        }

        stub.putStringState(key, gson.toJson(doctor));
        return "Doctor updated: " + doctor.getDoctorId();
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public String DeleteDoctor(Context ctx, String doctorId) {
        ChaincodeStub stub = ctx.getStub();
        String key = DOCTOR_PREFIX + doctorId;

        if (stub.getStringState(key).isEmpty()) {
            throw new RuntimeException("Doctor not found: " + doctorId);
        }

        stub.delState(key);
        return "🗑️ Doctor deleted successfully: " + doctorId;
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String GetAllDoctors(Context ctx) {
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
                    Record record = gson.fromJson(kv.getStringValue(), Record.class);
                    if (record != null && doctorId.equals(record.getDoctorId())) {
                        records.add(record);
                    }
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

        List<String> patientIds = new ArrayList<>();
        try (QueryResultsIterator<KeyValue> recordResults = stub.getStateByRange("", "")) {
            for (KeyValue kv : recordResults) {
                if (kv.getKey().startsWith(RECORD_PREFIX)) {
                    Record record = gson.fromJson(kv.getStringValue(), Record.class);
                    if (record != null && doctorId.equals(record.getDoctorId()) && record.getPatientId() != null) {
                        patientIds.add(record.getPatientId());
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving doctor records: " + e.getMessage());
        }

        try (QueryResultsIterator<KeyValue> patientResults = stub.getStateByRange("", "")) {
            for (KeyValue kv : patientResults) {
                if (kv.getKey().startsWith(PATIENT_PREFIX)) {
                    Patient patient = gson.fromJson(kv.getStringValue(), Patient.class);
                    if (patient != null && patientIds.contains(patient.getPatientId())) {
                        patients.add(patient);
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving patients: " + e.getMessage());
        }

        return gson.toJson(patients);
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public String RegisterDoctor(Context ctx, String doctorId, String name, String specialization, String hospitalId) {
        ChaincodeStub stub = ctx.getStub();

        Doctor doctor = new Doctor();
        doctor.setDoctorId(doctorId);
        doctor.setName(name);
        doctor.setSpecialization(specialization);
        doctor.setHospitalId(hospitalId);

        String key = DOCTOR_PREFIX + doctorId;
        if (!stub.getStringState(key).isEmpty()) {
            throw new RuntimeException("Doctor already exists: " + doctorId);
        }

        stub.putStringState(key, gson.toJson(doctor));
        return "Doctor registered successfully: " + name;
    }
}

