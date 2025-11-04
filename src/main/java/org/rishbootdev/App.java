package org.rishbootdev;

import org.hyperledger.fabric.contract.ContractRouter;

public class App {
    public static void main(String[] args) throws Exception {
        System.out.println("=== Starting HyperLedger Fabric Chaincode ===");
        ContractRouter.main(args);
    }
}
