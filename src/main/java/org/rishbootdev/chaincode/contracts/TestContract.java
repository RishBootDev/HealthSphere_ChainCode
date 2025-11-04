package org.rishbootdev.chaincode.contracts;


import com.google.gson.Gson;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Default;
import org.hyperledger.fabric.contract.annotation.Info;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.rishbootdev.chaincode.model.Hospital;
import org.rishbootdev.chaincode.model.Lab;
import org.rishbootdev.chaincode.model.Patient;
import org.rishbootdev.chaincode.model.Record;

import java.util.ArrayList;

@Contract(
        name="TestContract",
        info = @Info(
                title = "TestContract",
                description = "designed for the testing purposes",
                version = "1.0.0"
        )
)
@Default
public class TestContract implements ContractInterface {


    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String ping(Context ctx) {
        return "PONG: Chaincode is active and responding";
    }
    private static final Gson gson = new Gson();


    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public String createRecordTest(Context ctx, String key, String value) {
        ChaincodeStub stub = ctx.getStub();
        stub.putStringState(key, value);
        return "Record created successfully with key: " + key;
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String queryRecordTest(Context ctx, String key) {
        ChaincodeStub stub = ctx.getStub();
        String value = stub.getStringState(key);
        if (value == null || value.isEmpty()) {
            throw new ChaincodeException("Record not found for key: " + key);
        }
        return value;
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public String deleteRecordTest(Context ctx, String key) {
        ChaincodeStub stub = ctx.getStub();
        stub.delState(key);
        return "Record deleted successfully with key: " + key;
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String healthCheck(Context ctx) {
        return "Blockchain healthcare system operational";
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public String testLedgerData(Context ctx) {
        ChaincodeStub stub = ctx.getStub();
        try {
            Hospital hospital = new Hospital(
                    "HOSP1",
                    "Apollo Hospital",
                    "Bangalore",
                    "gov-HDFHORUE347387438738",
                    new ArrayList<>(),
                    new ArrayList<>(),
                    new ArrayList<>(),
                    new ArrayList<>()
            );
            stub.putStringState("HOSPITAL_" + hospital.getHospitalId(), gson.toJson(hospital));

            Lab lab = new Lab("LAB1", "Central Diagnostics", "HOSP1", new ArrayList<>());
            stub.putStringState("LAB_" + lab.getLabId(), gson.toJson(lab));

            Patient patient = new Patient(
                    "PAT1",
                    "John Doe",
                    35,
                    "Male",
                    "123 MG Road",
                    null,
                    "9876543210",
                    "O+",
                    "None",
                    new ArrayList<>(),
                    new ArrayList<>(),
                    null,
                    "HOSP1"
            );
            stub.putStringState("PATIENT_" + patient.getPatientId(), gson.toJson(patient));

            org.rishbootdev.chaincode.model.Record record = new Record(
                    "REC1",
                    "PAT1",
                    "DOC001",
                    "HOSP1",
                    "Checkup report",
                    "Blood pressure normal",
                    "Avoid stress",
                    "2025-11-03"
            );
            stub.putStringState("RECORD_" + record.getRecordId(), gson.toJson(record));

            return "Test data inserted into ledger successfully";
        } catch (Exception e) {
            throw new ChaincodeException("Failed to load test data: " + e.getMessage());
        }
    }
}
