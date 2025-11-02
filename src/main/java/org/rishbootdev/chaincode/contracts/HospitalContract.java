package org.rishbootdev.chaincode.contracts;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.annotation.*;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;
import org.rishbootdev.chaincode.model.*;

import java.util.ArrayList;
import java.util.List;

@Contract(
        name = "HospitalContract",
        info = @Info(
                title = "Hospital Contract",
                description = "Manages Hospital records and relationships with Doctors, Patients, Labs, and Records",
                version = "1.0.0"
        )
)
@Default
public class HospitalContract {

    private final Gson gson = new Gson();

    private static final String HOSP_PREFIX = "HOSPITAL_";
    private static final String DOCTOR_PREFIX = "DOCTOR_";
    private static final String PATIENT_PREFIX = "PATIENT_";
    private static final String LAB_PREFIX = "LAB_";
    private static final String RECORD_PREFIX = "RECORD_";


    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public Hospital createHospital(Context ctx, String hospitalId, String name, String address) {
        ChaincodeStub stub = ctx.getStub();
        if (stub.getStringState(hospitalId) != null && !stub.getStringState(hospitalId).isEmpty()) {
            throw new ChaincodeException("Hospital already exists with ID: " + hospitalId);
        }

        Hospital hospital = new Hospital(hospitalId, name, address,
                new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());

        stub.putStringState(hospitalId, gson.toJson(hospital));
        return hospital;
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public Hospital readHospital(Context ctx, String hospitalId) {
        ChaincodeStub stub = ctx.getStub();
        String hospitalJSON = stub.getStringState(hospitalId);

        if (hospitalJSON == null || hospitalJSON.isEmpty()) {
            throw new ChaincodeException("Hospital not found: " + hospitalId);
        }

        return gson.fromJson(hospitalJSON, Hospital.class);
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public Hospital updateHospital(Context ctx, String hospitalId, String name, String address) {
        Hospital hospital = readHospital(ctx, hospitalId);
        hospital.setName(name);
        hospital.setAddress(address);

        ctx.getStub().putStringState(hospitalId, gson.toJson(hospital));
        return hospital;
    }


    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public Hospital addDoctorToHospital(Context ctx, String hospitalId, String doctorId) {
        ChaincodeStub stub = ctx.getStub();

        Hospital hospital = readHospital(ctx, hospitalId);
        String doctorJSON = stub.getStringState(doctorId);

        if (doctorJSON == null || doctorJSON.isEmpty()) {
            throw new ChaincodeException("Doctor not found: " + doctorId);
        }

        Doctor doctor = gson.fromJson(doctorJSON, Doctor.class);

        if (!hospital.getDoctorIds().contains(doctorId)) {
            hospital.getDoctorIds().add(doctorId);
        }
        doctor.setHospitalId(hospitalId);
        stub.putStringState(hospitalId, gson.toJson(hospital));
        stub.putStringState(doctorId, gson.toJson(doctor));

        return hospital;
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public Hospital addPatientToHospital(Context ctx, String hospitalId, String patientId) {
        ChaincodeStub stub = ctx.getStub();

        Hospital hospital = readHospital(ctx, hospitalId);
        String patientJSON = stub.getStringState(patientId);

        if (patientJSON == null || patientJSON.isEmpty()) {
            throw new ChaincodeException("Patient not found: " + patientId);
        }

        Patient patient = gson.fromJson(patientJSON, Patient.class);

        if (!hospital.getPatientIds().contains(patientId)) {
            hospital.getPatientIds().add(patientId);
        }
        patient.setHospitalId(hospitalId);

        stub.putStringState(hospitalId, gson.toJson(hospital));
        stub.putStringState(patientId, gson.toJson(patient));

        return hospital;
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public Hospital addRecordToHospital(Context ctx, String hospitalId, String recordId) {
        ChaincodeStub stub = ctx.getStub();

        Hospital hospital = readHospital(ctx, hospitalId);
        String recordJSON = stub.getStringState(recordId);

        if (recordJSON == null || recordJSON.isEmpty()) {
            throw new ChaincodeException("Record not found: " + recordId);
        }

        Record record = gson.fromJson(recordJSON, Record.class);

        if (!hospital.getRecordId().contains(recordId)) {
            hospital.getRecordId().add(recordId);
        }
        record.setHospitalId(hospitalId);

        stub.putStringState(hospitalId, gson.toJson(hospital));
        stub.putStringState(recordId, gson.toJson(record));

        return hospital;
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public Hospital addLabToHospital(Context ctx, String hospitalId, String labId) {
        ChaincodeStub stub = ctx.getStub();

        Hospital hospital = readHospital(ctx, hospitalId);
        String labJSON = stub.getStringState(labId);

        if (labJSON == null || labJSON.isEmpty()) {
            throw new ChaincodeException("Lab not found: " + labId);
        }

        Lab lab = gson.fromJson(labJSON, Lab.class);

        if (!hospital.getLabId().contains(labId)) {
            hospital.getLabId().add(labId);
        }

        stub.putStringState(hospitalId, gson.toJson(hospital));
        stub.putStringState(labId, gson.toJson(lab));

        return hospital;
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void createHospital(Context ctx, String hospitalJson) {
        ChaincodeStub stub = ctx.getStub();
        Hospital hospital = gson.fromJson(hospitalJson, Hospital.class);

        if (hospital.getHospitalId() == null || hospital.getHospitalId().isEmpty()) {
            throw new RuntimeException("Hospital ID cannot be empty");
        }

        String key = HOSP_PREFIX + hospital.getHospitalId();
        if (!stub.getStringState(key).isEmpty()) {
            throw new RuntimeException("Hospital already exists: " + hospital.getHospitalId());
        }

        stub.putStringState(key, gson.toJson(hospital));
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String getHospital(Context ctx, String hospitalId) {
        String state = ctx.getStub().getStringState(HOSP_PREFIX + hospitalId);
        if (state == null || state.isEmpty()) {
            throw new RuntimeException("Hospital not found: " + hospitalId);
        }
        return state;
    }


    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void updateHospital(Context ctx, String hospitalJson) {
        ChaincodeStub stub = ctx.getStub();
        Hospital hospital = gson.fromJson(hospitalJson, Hospital.class);

        String key = HOSP_PREFIX + hospital.getHospitalId();
        if (stub.getStringState(key).isEmpty()) {
            throw new RuntimeException("Hospital not found: " + hospital.getHospitalId());
        }

        stub.putStringState(key, gson.toJson(hospital));
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void deleteHospital(Context ctx, String hospitalId) {
        ChaincodeStub stub = ctx.getStub();
        String key = HOSP_PREFIX + hospitalId;

        if (stub.getStringState(key).isEmpty()) {
            throw new RuntimeException("Hospital not found: " + hospitalId);
        }

        stub.delState(key);
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String getAllHospitals(Context ctx) {
        ChaincodeStub stub = ctx.getStub();
        List<Hospital> hospitals = new ArrayList<>();

        try (QueryResultsIterator<KeyValue> results = stub.getStateByRange(HOSP_PREFIX, HOSP_PREFIX + "z")) {
            for (KeyValue kv : results) {
                try {
                    Hospital h = gson.fromJson(kv.getStringValue(), Hospital.class);
                    if (h != null) hospitals.add(h);
                } catch (JsonSyntaxException ignored) {}
            }
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving hospitals: " + e.getMessage());
        }

        return gson.toJson(hospitals);
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String getDoctorsByHospital(Context ctx, String hospitalId) {
        ChaincodeStub stub = ctx.getStub();
        List<Doctor> doctors = new ArrayList<>();

        try (QueryResultsIterator<KeyValue> results = stub.getStateByRange(DOCTOR_PREFIX, DOCTOR_PREFIX + "z")) {
            for (KeyValue kv : results) {
                Doctor doctor = gson.fromJson(kv.getStringValue(), Doctor.class);
                if (doctor != null && hospitalId.equals(doctor.getHospitalId())) {
                    doctors.add(doctor);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving doctors: " + e.getMessage());
        }
        return gson.toJson(doctors);
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String getPatientsByHospital(Context ctx, String hospitalId) {
        ChaincodeStub stub = ctx.getStub();
        List<Patient> patients = new ArrayList<>();

        try (QueryResultsIterator<KeyValue> results = stub.getStateByRange(PATIENT_PREFIX, PATIENT_PREFIX + "z")) {
            for (KeyValue kv : results) {
                Patient patient = gson.fromJson(kv.getStringValue(), Patient.class);
                if (patient != null && hospitalId.equals(patient.getHospitalId())) {
                    patients.add(patient);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving patients: " + e.getMessage());
        }
        return gson.toJson(patients);
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String getAllLabs(Context ctx) {
        ChaincodeStub stub = ctx.getStub();
        List<Lab> labs = new ArrayList<>();

        try (QueryResultsIterator<KeyValue> results = stub.getStateByRange(LAB_PREFIX, LAB_PREFIX + "z")) {
            for (KeyValue kv : results) {
                Lab lab = gson.fromJson(kv.getStringValue(), Lab.class);
                if (lab != null) {
                    labs.add(lab);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving labs: " + e.getMessage());
        }
        return gson.toJson(labs);
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String getRecordsByHospital(Context ctx, String hospitalId) {
        ChaincodeStub stub = ctx.getStub();
        List<Record> records = new ArrayList<>();

        try (QueryResultsIterator<KeyValue> results = stub.getStateByRange(RECORD_PREFIX, RECORD_PREFIX + "z")) {
            for (KeyValue kv : results) {
                Record rec = gson.fromJson(kv.getStringValue(), Record.class);
                if (rec != null && hospitalId.equals(rec.getHospitalId())) {
                    records.add(rec);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving records: " + e.getMessage());
        }
        return gson.toJson(records);
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public String RegisterHospital(Context ctx, String hospitalJson) {
        ChaincodeStub stub = ctx.getStub();
        Hospital hospital = gson.fromJson(hospitalJson, Hospital.class);

        if (hospital.getHospitalId() == null || hospital.getHospitalId().isEmpty()) {
            throw new RuntimeException("Hospital ID cannot be empty");
        }

        String key = HOSP_PREFIX + hospital.getHospitalId();
        if (!stub.getStringState(key).isEmpty()) {
            throw new RuntimeException("Hospital already exists: " + hospital.getHospitalId());
        }

        stub.putStringState(key, gson.toJson(hospital));
        return "Hospital registered successfully: " + hospital.getName();
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String GetHospitalPatients(Context ctx) {
        ChaincodeStub stub = ctx.getStub();
        List<Patient> patients = new ArrayList<>();

        try (QueryResultsIterator<KeyValue> results = stub.getStateByRange(PATIENT_PREFIX, PATIENT_PREFIX + "z")) {
            for (KeyValue kv : results) {
                try {
                    Patient patient = gson.fromJson(kv.getStringValue(), Patient.class);
                    if (patient != null && patient.getPatientId() != null) {
                        patients.add(patient);
                    }
                } catch (JsonSyntaxException ignored) {}
            }
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving hospital patients: " + e.getMessage());
        }

        return gson.toJson(patients);
    }
}

