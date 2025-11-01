package org.rishbootdev;

import org.hyperledger.fabric.shim.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;

public class App extends ChaincodeBase {

    public static void main(String[] args,int a) throws IOException, InterruptedException {

        ChaincodeBase chaincode = new App();
//        ChaincodeServerProperties props = new ChaincodeServerProperties();
//        props.setServerAddress(new InetSocketAddress("localhost", 9999));
//        props.setMaxInboundMessageSize(104857600);
//        props.setMaxInboundMetadataSize(104857600);
//        props.setTlsEnabled(false);
//
//        NettyChaincodeServer server = new NettyChaincodeServer(chaincode, props);
        chaincode.start(args);
        System.out.println("Starting external chaincode gRPC server...");

        System.out.println("Starting chaincode event loop...");


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
