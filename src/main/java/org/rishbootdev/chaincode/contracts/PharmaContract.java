package org.rishbootdev.chaincode.contracts;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Default;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;
import org.rishbootdev.chaincode.model.Medicine;
import org.rishbootdev.chaincode.model.Pharma;

import java.util.ArrayList;
import java.util.List;

@Contract(name = "PharmaContract")
@Default
public class PharmaContract {

    private final Gson gson = new Gson();

    private static final String PHARMA_PREFIX = "PHARMA_";
    private static final String MEDICINE_PREFIX = "MEDICINE_";

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

        if (pharma.getMedicineId() == null) {
            pharma.setMedicineId(new ArrayList<>());
        }

        stub.putStringState(key, gson.toJson(pharma));
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String getPharma(Context ctx, String pharmaId) {
        String key = PHARMA_PREFIX + pharmaId;
        String json = ctx.getStub().getStringState(key);

        if (json == null || json.isEmpty()) {
            throw new RuntimeException("Pharma not found: " + pharmaId);
        }
        return json;
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String getAllPharmas(Context ctx) {
        ChaincodeStub stub = ctx.getStub();
        List<Pharma> pharmaList = new ArrayList<>();

        try (QueryResultsIterator<KeyValue> results =
                     stub.getStateByRange(PHARMA_PREFIX, PHARMA_PREFIX + "\uFFFF")) {
            for (KeyValue kv : results) {
                try {
                    Pharma pharma = gson.fromJson(kv.getStringValue(), Pharma.class);
                    if (pharma != null && pharma.getPharmaId() != null) {
                        pharmaList.add(pharma);
                    }
                } catch (JsonSyntaxException ignored) {}
            }
        } catch (Exception e) {
            throw new RuntimeException("Error retrieving pharmas: " + e.getMessage());
        }

        return gson.toJson(pharmaList);
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

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void deletePharma(Context ctx, String pharmaId) {
        ChaincodeStub stub = ctx.getStub();
        String key = PHARMA_PREFIX + pharmaId;

        String json = stub.getStringState(key);
        if (json == null || json.isEmpty()) {
            throw new RuntimeException("Pharma not found: " + pharmaId);
        }

        Pharma pharma = gson.fromJson(json, Pharma.class);

        if (pharma.getMedicineId() != null) {
            for (String medId : pharma.getMedicineId()) {
                String medKey = MEDICINE_PREFIX + medId;
                String medJson = stub.getStringState(medKey);
                if (medJson != null && !medJson.isEmpty()) {
                    Medicine med = gson.fromJson(medJson, Medicine.class);
                    stub.putStringState(medKey, gson.toJson(med));
                }
            }
        }

        stub.delState(key);
    }
    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void addMedicineToPharma(Context ctx, String pharmaId, String medicineId) {
        ChaincodeStub stub = ctx.getStub();
        String pharmaKey = PHARMA_PREFIX + pharmaId;
        String pharmaJson = stub.getStringState(pharmaKey);

        if (pharmaJson == null || pharmaJson.isEmpty()) {
            throw new RuntimeException("Pharma not found: " + pharmaId);
        }

        String medKey = MEDICINE_PREFIX + medicineId;
        String medJson = stub.getStringState(medKey);
        if (medJson == null || medJson.isEmpty()) {
            throw new RuntimeException("Medicine not found: " + medicineId);
        }

        Pharma pharma = gson.fromJson(pharmaJson, Pharma.class);
        List<String> medList = pharma.getMedicineId();
        if (medList == null) {
            medList = new ArrayList<>();
        }

        if (!medList.contains(medicineId)) {
            medList.add(medicineId);
            pharma.setMedicineId(medList);
            stub.putStringState(pharmaKey, gson.toJson(pharma));
        }
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void removeMedicineFromPharma(Context ctx, String pharmaId, String medicineId) {
        ChaincodeStub stub = ctx.getStub();
        String pharmaKey = PHARMA_PREFIX + pharmaId;
        String pharmaJson = stub.getStringState(pharmaKey);

        if (pharmaJson == null || pharmaJson.isEmpty()) {
            throw new RuntimeException("Pharma not found: " + pharmaId);
        }

        Pharma pharma = gson.fromJson(pharmaJson, Pharma.class);
        List<String> medList = pharma.getMedicineId();

        if (medList != null && medList.remove(medicineId)) {
            pharma.setMedicineId(medList);
            stub.putStringState(pharmaKey, gson.toJson(pharma));
        }
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String getMedicinesByPharma(Context ctx, String pharmaId) {
        ChaincodeStub stub = ctx.getStub();
        String pharmaKey = PHARMA_PREFIX + pharmaId;
        String pharmaJson = stub.getStringState(pharmaKey);

        if (pharmaJson == null || pharmaJson.isEmpty()) {
            throw new RuntimeException("Pharma not found: " + pharmaId);
        }

        Pharma pharma = gson.fromJson(pharmaJson, Pharma.class);
        List<String> medicineIds = pharma.getMedicineId();
        List<Medicine> medicines = new ArrayList<>();

        if (medicineIds != null) {
            for (String medId : medicineIds) {
                String medJson = stub.getStringState(MEDICINE_PREFIX + medId);
                if (medJson != null && !medJson.isEmpty()) {
                    try {
                        medicines.add(gson.fromJson(medJson, Medicine.class));
                    } catch (JsonSyntaxException ignored) {}
                }
            }
        }

        return gson.toJson(medicines);
    }
}
