package org.rishbootdev;

import com.google.gson.Gson;
import org.hyperledger.fabric.contract.ContractRouter;

public class App {

    private static final Gson gson = new Gson();

    public static void main(String[] args) throws Exception {
        System.out.println("=== Starting HyperLedger Fabric Chaincode ===");
        ContractRouter.main(args);

    }
}
