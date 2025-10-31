package org.rishbootdev.chaincode.contracts;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.rishbootdev.chaincode.model.Medicine;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;

import java.util.ArrayList;
import java.util.List;

@Contract(name = "MedicineContract")
public class MedicineContract {

    private final Gson gson = new Gson();

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void createMedicine(Context ctx, String medicineJson) {
        ChaincodeStub stub = ctx.getStub();
        Medicine medicine = gson.fromJson(medicineJson, Medicine.class);

        if (medicine.getId() == null || medicine.getId().isEmpty()) {
            throw new RuntimeException("Medicine ID cannot be empty");
        }

        String key = "MEDICINE_" + medicine.getId();

        if (!stub.getStringState(key).isEmpty()) {
            throw new RuntimeException("Medicine already exists: " + medicine.getId());
        }

        stub.putStringState(key, gson.toJson(medicine));
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String getAllMedicines(Context ctx) {
        ChaincodeStub stub = ctx.getStub();
        List<Medicine> medicines = new ArrayList<>();

        String startKey = "MEDICINE_";
        String endKey = "MEDICINE_\uFFFF";

        try (QueryResultsIterator<KeyValue> results = stub.getStateByRange(startKey, endKey)) {
            for (KeyValue kv : results) {
                String json = kv.getStringValue();
                if (json == null || json.isEmpty()) continue;

                try {
                    Medicine med = gson.fromJson(json, Medicine.class);
                    if (med != null && med.getId() != null && !med.getId().isEmpty()) {
                        medicines.add(med);
                    }
                } catch (JsonSyntaxException ex) {
                    System.out.println("Skipping invalid Medicine JSON: " + ex.getMessage());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error reading Medicines from ledger: " + e.getMessage());
        }

        return gson.toJson(medicines);
    }


    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String searchMedicineByName(Context ctx, String name) throws Exception {
        ChaincodeStub stub = ctx.getStub();
        List<Medicine> found = new ArrayList<>();

        String startKey = "MEDICINE_";
        String endKey = "MEDICINE_\uFFFF";

        try (QueryResultsIterator<KeyValue> results = stub.getStateByRange(startKey, endKey)) {
            for (KeyValue kv : results) {
                try {
                    Medicine med = gson.fromJson(kv.getStringValue(), Medicine.class);
                    if (med != null && name.equalsIgnoreCase(med.getName())) {
                        found.add(med);
                    }
                } catch (Exception ex) {
                    System.out.println("Skipping malformed record while searching: " + ex.getMessage());
                }
            }
        }

        if (found.isEmpty()) {
            throw new RuntimeException("No medicine found with name: " + name);
        }

        return gson.toJson(found);
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void updateMedicineStock(Context ctx, String medicineId, int stock) {
        ChaincodeStub stub = ctx.getStub();
        String key = "MEDICINE_" + medicineId;
        String medJson = stub.getStringState(key);

        if (medJson == null || medJson.isEmpty()) {
            throw new RuntimeException("Medicine not found: " + medicineId);
        }

        Medicine med = gson.fromJson(medJson, Medicine.class);
        med.setStock(stock);
        stub.putStringState(key, gson.toJson(med));
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String getMedicine(Context ctx, String medicineId) {
        String key = "MEDICINE_" + medicineId;
        String medJson = ctx.getStub().getStringState(key);
        if (medJson == null || medJson.isEmpty()) {
            throw new RuntimeException("Medicine not found: " + medicineId);
        }
        return medJson;
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void deleteMedicine(Context ctx, String medicineId) {
        ChaincodeStub stub = ctx.getStub();
        String key = "MEDICINE_" + medicineId;
        String existing = stub.getStringState(key);

        if (existing == null || existing.isEmpty()) {
            throw new RuntimeException("Medicine not found: " + medicineId);
        }

        stub.delState(key);
    }
}
