package org.rishbootdev.chaincode.contracts;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
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
                title = "HospitalContract",
                description = "Manages Hospital records and relationships with Doctors, Patients, Labs, and Records",
                version = "1.1.0"
        )
)
@Default
public class HospitalContract implements ContractInterface {

    private final Gson gson = new Gson();

    private static final String HOSP_PREFIX = "HOSPITAL_";
    private static final String DOCTOR_PREFIX = "DOCTOR_";
    private static final String PATIENT_PREFIX = "PATIENT_";
    private static final String LAB_PREFIX = "LAB_";
    private static final String RECORD_PREFIX = "RECORD_";


    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public Hospital createHospital(Context ctx, String hospitalId, String name, String address, String license) {
        ChaincodeStub stub = ctx.getStub();
        String key = HOSP_PREFIX + hospitalId;

        String existing = stub.getStringState(key);
        if (existing != null && !existing.isEmpty()) {
            throw new ChaincodeException("Hospital already exists with ID: " + hospitalId);
        }

        Hospital hospital = new Hospital(
                hospitalId,
                name,
                address,
                license,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>()
        );

        stub.putStringState(key, gson.toJson(hospital));
        return hospital;
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public Hospital readHospital(Context ctx, String hospitalId) {
        ChaincodeStub stub = ctx.getStub();
        String key = HOSP_PREFIX + hospitalId;

        String hospitalJSON = stub.getStringState(key);
        if (hospitalJSON == null || hospitalJSON.isEmpty()) {
            throw new ChaincodeException("Hospital not found: " + hospitalId);
        }

        return gson.fromJson(hospitalJSON, Hospital.class);
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public Hospital updateHospital(Context ctx, String hospitalId, String name, String address) {
        ChaincodeStub stub = ctx.getStub();
        String key = HOSP_PREFIX + hospitalId;

        Hospital hospital = readHospital(ctx, hospitalId);
        hospital.setName(name);
        hospital.setAddress(address);

        stub.putStringState(key, gson.toJson(hospital));
        return hospital;
    }
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public Hospital addDoctorToHospital(Context ctx, String hospitalId, String doctorId) {
        ChaincodeStub stub = ctx.getStub();

        Hospital hospital = readHospital(ctx, hospitalId);
        String doctorJSON = stub.getStringState(DOCTOR_PREFIX + doctorId);

        if (doctorJSON == null || doctorJSON.isEmpty()) {
            throw new ChaincodeException("Doctor not found: " + doctorId);
        }

        Doctor doctor = gson.fromJson(doctorJSON, Doctor.class);

        if (!hospital.getDoctorIds().contains(doctorId)) {
            hospital.getDoctorIds().add(doctorId);
        }
        doctor.setHospitalId(hospitalId);

        stub.putStringState(HOSP_PREFIX + hospitalId, gson.toJson(hospital));
        stub.putStringState(DOCTOR_PREFIX + doctorId, gson.toJson(doctor));

        return hospital;
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public Hospital addPatientToHospital(Context ctx, String hospitalId, String patientId) {
        ChaincodeStub stub = ctx.getStub();

        Hospital hospital = readHospital(ctx, hospitalId);
        String patientJSON = stub.getStringState(PATIENT_PREFIX + patientId);

        if (patientJSON == null || patientJSON.isEmpty()) {
            throw new ChaincodeException("Patient not found: " + patientId);
        }

        Patient patient = gson.fromJson(patientJSON, Patient.class);

        if (!hospital.getPatientIds().contains(patientId)) {
            hospital.getPatientIds().add(patientId);
        }
        patient.setHospitalId(hospitalId);

        stub.putStringState(HOSP_PREFIX + hospitalId, gson.toJson(hospital));
        stub.putStringState(PATIENT_PREFIX + patientId, gson.toJson(patient));

        return hospital;
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public Hospital addRecordToHospital(Context ctx, String hospitalId, String recordId) {
        ChaincodeStub stub = ctx.getStub();

        Hospital hospital = readHospital(ctx, hospitalId);
        String recordJSON = stub.getStringState(RECORD_PREFIX + recordId);

        if (recordJSON == null || recordJSON.isEmpty()) {
            throw new ChaincodeException("Record not found: " + recordId);
        }

        org.rishbootdev.chaincode.model.Record record = gson.fromJson(recordJSON,  org.rishbootdev.chaincode.model.Record.class);

        if (!hospital.getRecordIds().contains(recordId)) {
            hospital.getRecordIds().add(recordId);
        }
        record.setHospitalId(hospitalId);

        stub.putStringState(HOSP_PREFIX + hospitalId, gson.toJson(hospital));
        stub.putStringState(RECORD_PREFIX + recordId, gson.toJson(record));

        return hospital;
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public Hospital addLabToHospital(Context ctx, String hospitalId, String labId) {
        ChaincodeStub stub = ctx.getStub();

        String hospitalKey = HOSP_PREFIX + hospitalId;
        String labKey = LAB_PREFIX + labId;

        String hospitalJSON = stub.getStringState(hospitalKey);
        if (hospitalJSON == null || hospitalJSON.isEmpty()) {
            throw new ChaincodeException("Hospital not found: " + hospitalId);
        }
        Hospital hospital = gson.fromJson(hospitalJSON, Hospital.class);

        String labJSON = stub.getStringState(labKey);
        if (labJSON == null || labJSON.isEmpty()) {
            throw new ChaincodeException("Lab not found: " + labId);
        }
        Lab lab = gson.fromJson(labJSON, Lab.class);

        if (!hospital.getLabIds().contains(labId)) {
            hospital.getLabIds().add(labId);
        }

        lab.setHospitalId(hospitalId);
        stub.putStringState(hospitalKey, gson.toJson(hospital));
        stub.putStringState(labKey, gson.toJson(lab));

        return hospital;
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void createHospitalBody(Context ctx, String hospitalJson) {
        ChaincodeStub stub = ctx.getStub();
        Hospital hospital = gson.fromJson(hospitalJson, Hospital.class);

        if (hospital.getHospitalId() == null || hospital.getHospitalId().isEmpty()) {
            throw new ChaincodeException("Hospital ID cannot be empty");
        }

        String key = HOSP_PREFIX + hospital.getHospitalId();
        String existing = stub.getStringState(key);
        if (existing != null && !existing.isEmpty()) {
            throw new ChaincodeException("Hospital already exists: " + hospital.getHospitalId());
        }

        stub.putStringState(key, gson.toJson(hospital));
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String getHospitalById(Context ctx, String hospitalId) {
        String state = ctx.getStub().getStringState(HOSP_PREFIX + hospitalId);
        if (state == null || state.isEmpty()) {
            throw new ChaincodeException("Hospital not found: " + hospitalId);
        }
        return state;
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void updateHospitalBody(Context ctx, String hospitalJson) {
        ChaincodeStub stub = ctx.getStub();
        Hospital hospital = gson.fromJson(hospitalJson, Hospital.class);

        String key = HOSP_PREFIX + hospital.getHospitalId();
        String existing = stub.getStringState(key);
        if (existing == null || existing.isEmpty()) {
            throw new ChaincodeException("Hospital not found: " + hospital.getHospitalId());
        }

        stub.putStringState(key, gson.toJson(hospital));
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void deleteHospitalById(Context ctx, String hospitalId) {
        ChaincodeStub stub = ctx.getStub();
        String key = HOSP_PREFIX + hospitalId;

        String existing = stub.getStringState(key);
        if (existing == null || existing.isEmpty()) {
            throw new ChaincodeException("Hospital not found: " + hospitalId);
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
            throw new ChaincodeException("Error retrieving hospitals: " + e.getMessage());
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
            throw new ChaincodeException("Error retrieving doctors: " + e.getMessage());
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
            throw new ChaincodeException("Error retrieving patients: " + e.getMessage());
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
            throw new ChaincodeException("Error retrieving labs: " + e.getMessage());
        }
        return gson.toJson(labs);
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String getRecordsByHospital(Context ctx, String hospitalId) {
        ChaincodeStub stub = ctx.getStub();
        List< org.rishbootdev.chaincode.model.Record> records = new ArrayList<>();

        try (QueryResultsIterator<KeyValue> results = stub.getStateByRange(RECORD_PREFIX, RECORD_PREFIX + "z")) {
            for (KeyValue kv : results) {
                org.rishbootdev.chaincode.model.Record rec = gson.fromJson(kv.getStringValue(),  org.rishbootdev.chaincode.model.Record.class);
                if (rec != null && hospitalId.equals(rec.getHospitalId())) {
                    records.add(rec);
                }
            }
        } catch (Exception e) {
            throw new ChaincodeException("Error retrieving records: " + e.getMessage());
        }
        return gson.toJson(records);
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public String registerHospital(Context ctx, String hospitalJson) {
        ChaincodeStub stub = ctx.getStub();
        Hospital hospital = gson.fromJson(hospitalJson, Hospital.class);

        if (hospital.getHospitalId() == null || hospital.getHospitalId().isEmpty()) {
            throw new ChaincodeException("Hospital ID cannot be empty");
        }

        String key = HOSP_PREFIX + hospital.getHospitalId();
        String existing = stub.getStringState(key);
        if (existing != null && !existing.isEmpty()) {
            throw new ChaincodeException("Hospital already exists: " + hospital.getHospitalId());
        }

        stub.putStringState(key, gson.toJson(hospital));
        return "Hospital registered successfully: " + hospital.getName();
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String getHospitalPatients(Context ctx) {
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
            throw new ChaincodeException("Error retrieving hospital patients: " + e.getMessage());
        }

        return gson.toJson(patients);
    }
}
