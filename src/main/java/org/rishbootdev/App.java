package org.rishbootdev;

import org.hyperledger.fabric.shim.*;

import java.io.IOException;

import java.util.List;

public class App extends ChaincodeBase {
    public static void main(String[] args) throws IOException, InterruptedException {

        App chaincode = new App();

        chaincode.start(args);


//        String chaincodeId = System.getenv("CORE_CHAINCODE_ID_NAME");
//        if (chaincodeId == null || chaincodeId.isEmpty()) {
//            throw new IllegalArgumentException("CORE_CHAINCODE_ID_NAME environment variable is required");
//        }
//
//        String address = System.getenv().getOrDefault("CHAINCODE_SERVER_ADDRESS", "0.0.0.0:9999");
//        String[] parts = address.split(":");
//        String host = parts[0];
//        int port = Integer.parseInt(parts[1]);

//        ChaincodeServerProperties props = new ChaincodeServerProperties();
//        props.setServerAddress(new InetSocketAddress(host, port));
//        props.setTlsEnabled(false);
//
//        NettyChaincodeServer server = new NettyChaincodeServer(chaincode, props);
//        server.start();
//
//        Thread.currentThread().join();
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
