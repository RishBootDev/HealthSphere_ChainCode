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

@Contract(name = "MedicineContract")
@Default
public class MedicineContract {

    private final Gson gson = new Gson();

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void createMedicine(Context ctx, String medicineJson) {
        ChaincodeStub stub = ctx.getStub();
        Medicine medicine = gson.fromJson(medicineJson, Medicine.class);

        if (medicine.getId() == null || medicine.getId().isEmpty()) {
            throw new RuntimeException("Medicine ID cannot be empty");
        }
        if (!stub.getStringState(medicine.getId()).isEmpty()) {
            throw new RuntimeException("Medicine already exists: " + medicine.getId());
        }
        stub.putStringState(medicine.getId(), gson.toJson(medicine));
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String readMedicine(Context ctx, String medicineId) {
        String json = ctx.getStub().getStringState(medicineId);
        if (json == null || json.isEmpty()) {
            throw new RuntimeException("Medicine not found: " + medicineId);
        }
        return json;
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String getAllMedicines(Context ctx) {
        ChaincodeStub stub = ctx.getStub();
        List<Medicine> medicines = new ArrayList<>();

        try (QueryResultsIterator<KeyValue> results = stub.getStateByRange("", "")) {
            for (KeyValue kv : results) {
                try {
                    Medicine med = gson.fromJson(kv.getStringValue(), Medicine.class);
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

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void updateMedicine(Context ctx, String medicineJson) {
        ChaincodeStub stub = ctx.getStub();
        Medicine medicine = gson.fromJson(medicineJson, Medicine.class);

        if (medicine.getId() == null || medicine.getId().isEmpty()) {
            throw new RuntimeException("Medicine ID cannot be empty");
        }

        String existing = stub.getStringState(medicine.getId());
        if (existing == null || existing.isEmpty()) {
            throw new RuntimeException("Medicine not found: " + medicine.getId());
        }

        stub.putStringState(medicine.getId(), gson.toJson(medicine));
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void deleteMedicine(Context ctx, String medicineId) {
        ChaincodeStub stub = ctx.getStub();
        String existing = stub.getStringState(medicineId);

        if (existing == null || existing.isEmpty()) {
            throw new RuntimeException("Medicine not found: " + medicineId);
        }

        // Remove medicine from any Pharma relationships
        try (QueryResultsIterator<KeyValue> results = stub.getStateByRange("", "")) {
            for (KeyValue kv : results) {
                try {
                    Pharma pharma = gson.fromJson(kv.getStringValue(), Pharma.class);
                    if (pharma != null && pharma.getMedicineId() != null &&
                            pharma.getMedicineId().contains(medicineId)) {
                        pharma.getMedicineId().remove(medicineId);
                        stub.putStringState(pharma.getPharmaId(), gson.toJson(pharma));
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}

        stub.delState(medicineId);
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String searchMedicineByName(Context ctx, String name) throws Exception {
        ChaincodeStub stub = ctx.getStub();
        List<Medicine> found = new ArrayList<>();

        try (QueryResultsIterator<KeyValue> results = stub.getStateByRange("", "")) {
            for (KeyValue kv : results) {
                try {
                    Medicine med = gson.fromJson(kv.getStringValue(), Medicine.class);
                    if (med != null && med.getName() != null &&
                            med.getName().equalsIgnoreCase(name)) {
                        found.add(med);
                    }
                } catch (Exception ex) {
                    System.out.println("Skipping malformed record: " + ex.getMessage());
                }
            }
        }

        if (found.isEmpty()) {
            throw new RuntimeException("No medicine found with name: " + name);
        }

        return gson.toJson(found);
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void updateMedicineStock(Context ctx, String medicineId, int newStock) {
        ChaincodeStub stub = ctx.getStub();
        String json = stub.getStringState(medicineId);

        if (json == null || json.isEmpty()) {
            throw new RuntimeException("Medicine not found: " + medicineId);
        }

        Medicine med = gson.fromJson(json, Medicine.class);
        med.setStock(newStock);
        stub.putStringState(medicineId, gson.toJson(med));
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void addMedicineToPharma(Context ctx, String pharmaId, String medicineId) {
        ChaincodeStub stub = ctx.getStub();

        String pharmaJson = stub.getStringState(pharmaId);
        if (pharmaJson == null || pharmaJson.isEmpty()) {
            throw new RuntimeException("Pharma not found: " + pharmaId);
        }

        String medJson = stub.getStringState(medicineId);
        if (medJson == null || medJson.isEmpty()) {
            throw new RuntimeException("Medicine not found: " + medicineId);
        }

        Pharma pharma = gson.fromJson(pharmaJson, Pharma.class);
        List<String> meds = pharma.getMedicineId();
        if (!meds.contains(medicineId)) {
            meds.add(medicineId);
        }

        stub.putStringState(pharmaId, gson.toJson(pharma));
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public void removeMedicineFromPharma(Context ctx, String pharmaId, String medicineId) {
        ChaincodeStub stub = ctx.getStub();
        String pharmaJson = stub.getStringState(pharmaId);

        if (pharmaJson == null || pharmaJson.isEmpty()) {
            throw new RuntimeException("Pharma not found: " + pharmaId);
        }

        Pharma pharma = gson.fromJson(pharmaJson, Pharma.class);
        List<String> meds = pharma.getMedicineId();
        meds.remove(medicineId);
        stub.putStringState(pharmaId, gson.toJson(pharma));
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String getMedicinesByPharma(Context ctx, String pharmaId) {
        ChaincodeStub stub = ctx.getStub();
        String pharmaJson = stub.getStringState(pharmaId);

        if (pharmaJson == null || pharmaJson.isEmpty()) {
            throw new RuntimeException("Pharma not found: " + pharmaId);
        }

        Pharma pharma = gson.fromJson(pharmaJson, Pharma.class);
        List<Medicine> medicines = new ArrayList<>();

        for (String medId : pharma.getMedicineId()) {
            String medJson = stub.getStringState(medId);
            if (medJson != null && !medJson.isEmpty()) {
                medicines.add(gson.fromJson(medJson, Medicine.class));
            }
        }

        return gson.toJson(medicines);
    }
}
