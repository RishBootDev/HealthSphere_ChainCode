package org.rishbootdev;

import org.hyperledger.fabric.shim.ChaincodeBase;
import org.hyperledger.fabric.shim.ChaincodeStub;
import java.util.List;

public class App extends ChaincodeBase {

    public static void main(String[] args) {
        new App().start(args);
    }

    @Override
    public Response init(ChaincodeStub stub) {
        System.out.println("=== Chaincode initialization ===");
        return newSuccessResponse("Chaincode initialized successfully");
    }

    @Override
    public Response invoke(ChaincodeStub stub) {
        String function = stub.getFunction();
        List<String> params = stub.getParameters();

        switch (function) {
            case "createRecord":
                return createRecord(stub, params);
            case "queryRecord":
                return queryRecord(stub, params);
            case "deleteRecord":
                return deleteRecord(stub, params);
            default:
                return newErrorResponse("Invalid function name: " + function);
        }
    }

    private Response createRecord(ChaincodeStub stub, List<String> params) {
        if (params.size() < 2) {
            return newErrorResponse("Incorrect number of arguments. Expecting 2");
        }
        String key = params.get(0);
        String value = params.get(1);
        stub.putStringState(key, value);
        return newSuccessResponse("Record created successfully");
    }

    private Response queryRecord(ChaincodeStub stub, List<String> params) {
        if (params.size() < 1) {
            return newErrorResponse("Incorrect number of arguments. Expecting 1");
        }
        String key = params.get(0);
        String value = stub.getStringState(key);
        if (value == null || value.isEmpty()) {
            return newErrorResponse("Record not found for key: " + key);
        }
        return newSuccessResponse(value);
    }

    private Response deleteRecord(ChaincodeStub stub, List<String> params) {
        if (params.size() < 1) {
            return newErrorResponse("Incorrect number of arguments. Expecting 1");
        }
        String key = params.get(0);
        stub.delState(key);
        return newSuccessResponse("Record deleted successfully");
    }
}
