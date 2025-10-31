package org.rishbootdev.chaincode.contracts;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.rishbootdev.chaincode.model.Pharma;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;

import java.util.ArrayList;
import java.util.List;

@Contract(name = "PharmaContract")
public class PharmaContract {

    private final Gson gson = new Gson();
    private static final String PHARMA_PREFIX = "PHARMA_";

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void createPharma(Context ctx, String pharmaJson) {
        ChaincodeStub stub = ctx.getStub();
        Pharma pharma = gson.fromJson(pharmaJson, Pharma.class);

        if (pharma.getPharmaId() == null || pharma.getPharmaId().isEmpty()) {
            throw new RuntimeException("Pharma ID cannot be empty");
        }

        String key = PHARMA_PREFIX + pharma.getPharmaId();

        if (!stub.getStringState(key).isEmpty()) {
            throw new RuntimeException("Pharma already exists: " + pharma.getPharmaId());
        }

        stub.putStringState(key, gson.toJson(pharma));
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String getPharma(Context ctx, String pharmaId) {
        String key = PHARMA_PREFIX + pharmaId;
        String pharmaState = ctx.getStub().getStringState(key);
        if (pharmaState == null || pharmaState.isEmpty()) {
            throw new RuntimeException("Pharma not found: " + pharmaId);
        }
        return pharmaState;
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void updatePharma(Context ctx, String pharmaJson) {
        ChaincodeStub stub = ctx.getStub();
        Pharma pharma = gson.fromJson(pharmaJson, Pharma.class);

        String key = PHARMA_PREFIX + pharma.getPharmaId();
        if (stub.getStringState(key).isEmpty()) {
            throw new RuntimeException("Pharma not found: " + pharma.getPharmaId());
        }

        stub.putStringState(key, gson.toJson(pharma));
    }


    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String getAllPharmas(Context ctx) {
        ChaincodeStub stub = ctx.getStub();
        List<Pharma> pharmaList = new ArrayList<>();

        try (QueryResultsIterator<KeyValue> results =
                     stub.getStateByRange(PHARMA_PREFIX, PHARMA_PREFIX + "\uFFFF")) {
            for (KeyValue kv : results) {
                String json = kv.getStringValue();
                if (json == null || json.isEmpty()) continue;

                try {
                    Pharma pharma = gson.fromJson(json, Pharma.class);
                    if (pharma != null && pharma.getPharmaId() != null && !pharma.getPharmaId().isEmpty()) {
                        pharmaList.add(pharma);
                    }
                } catch (JsonSyntaxException ex) {
                    System.out.println("Skipping invalid Pharma JSON: " + ex.getMessage());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error reading Pharma data from ledger: " + e.getMessage());
        }

        return gson.toJson(pharmaList);
    }


    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void addMedicineToPharma(Context ctx, String pharmaId, String medicineId) {
        ChaincodeStub stub = ctx.getStub();
        String key = PHARMA_PREFIX + pharmaId;
        String pharmaJson = stub.getStringState(key);

        if (pharmaJson == null || pharmaJson.isEmpty()) {
            throw new RuntimeException("Pharma not found: " + pharmaId);
        }

        Pharma pharma = gson.fromJson(pharmaJson, Pharma.class);
        List<String> medicineIds = pharma.getMedicineId();

        if (medicineIds == null) {
            medicineIds = new ArrayList<>();
            pharma.setMedicineId(medicineIds);
        }

        if (!medicineIds.contains(medicineId)) {
            medicineIds.add(medicineId);
            stub.putStringState(key, gson.toJson(pharma));
        }
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void removeMedicineFromPharma(Context ctx, String pharmaId, String medicineId) {
        ChaincodeStub stub = ctx.getStub();
        String key = PHARMA_PREFIX + pharmaId;
        String pharmaJson = stub.getStringState(key);

        if (pharmaJson == null || pharmaJson.isEmpty()) {
            throw new RuntimeException("Pharma not found: " + pharmaId);
        }

        Pharma pharma = gson.fromJson(pharmaJson, Pharma.class);
        List<String> medicineIds = pharma.getMedicineId();

        if (medicineIds != null && medicineIds.remove(medicineId)) {
            pharma.setMedicineId(medicineIds);
            stub.putStringState(key, gson.toJson(pharma));
        }
    }
}
