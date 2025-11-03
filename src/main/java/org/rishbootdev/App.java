package org.rishbootdev;

import com.google.gson.Gson;
import org.hyperledger.fabric.contract.ContractRouter;
import org.hyperledger.fabric.shim.ChaincodeBase;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.rishbootdev.chaincode.model.Hospital;
import org.rishbootdev.chaincode.model.Lab;
import org.rishbootdev.chaincode.model.Patient;
import org.rishbootdev.chaincode.model.Record;

import java.util.ArrayList;
import java.util.List;

public class App extends ChaincodeBase {

    private static final Gson gson = new Gson();

    public static void main(String[] args) throws Exception {
        System.out.println("=== Starting HyperLedger Fabric Chaincode ===");
        ContractRouter.main(args);
        App chaincode = new App();
        chaincode.start(args);
    }

    @Override
    public Response init(ChaincodeStub stub) {
        System.out.println("=== Chaincode initialization successful ===");
        return newSuccessResponse("Chaincode initialized successfully");
    }

    @Override
    public Response invoke(ChaincodeStub stub) {
        String function = stub.getFunction();
        List<String> params = stub.getParameters();

        System.out.println("Invoking function: " + function);
        System.out.println("Parameters: " + params);

        switch (function) {
            case "createRecord":
                return createRecord(stub, params);
            case "queryRecord":
                return queryRecord(stub, params);
            case "deleteRecord":
                return deleteRecord(stub, params);
            case "ping":
                return newSuccessResponse("PONG: Chaincode is active and responding");
            case "healthCheck":
                return newSuccessResponse("Blockchain healthcare system operational");
            case "testLedgerData":
                return testLedgerData(stub);
            default:
                return newErrorResponse("Invalid function name or contract not found: " + function);
        }
    }

    private Response createRecord(ChaincodeStub stub, List<String> params) {
        if (params.size() < 2)
            return newErrorResponse("Incorrect number of arguments. Expecting 2: [key, value]");
        String key = params.get(0);
        String value = params.get(1);
        stub.putStringState(key, value);
        return newSuccessResponse("Record created successfully with key: " + key);
    }

    private Response queryRecord(ChaincodeStub stub, List<String> params) {
        if (params.size() < 1)
            return newErrorResponse("Incorrect number of arguments. Expecting 1: [key]");
        String key = params.get(0);
        String value = stub.getStringState(key);
        if (value == null || value.isEmpty())
            return newErrorResponse("Record not found for key: " + key);
        return newSuccessResponse(value);
    }

    private Response deleteRecord(ChaincodeStub stub, List<String> params) {
        if (params.size() < 1)
            return newErrorResponse("Incorrect number of arguments. Expecting 1: [key]");
        String key = params.get(0);
        stub.delState(key);
        return newSuccessResponse("Record deleted successfully with key: " + key);
    }

    private Response testLedgerData(ChaincodeStub stub) {
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

            Record record = new Record(
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

            return newSuccessResponse("Test data inserted into ledger successfully");
        } catch (Exception e) {
            return newErrorResponse("Failed to load test data: " + e.getMessage());
        }
    }
}
