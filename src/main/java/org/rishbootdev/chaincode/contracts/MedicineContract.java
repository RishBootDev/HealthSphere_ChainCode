package org.rishbootdev.chaincode.contracts;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Default;
import org.hyperledger.fabric.contract.annotation.Info;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.ChaincodeStub;
import org.hyperledger.fabric.shim.ledger.KeyValue;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.rishbootdev.chaincode.model.Medicine;
import org.rishbootdev.chaincode.model.Pharma;

import java.util.ArrayList;
import java.util.List;

@Contract(
        name = "MedicineContract",
        info = @Info(
                title = "Medicine Contract",
                description = "Manages all the operations related to the medicines",
                version = "1.0.0"
        )
)
@Default
public class MedicineContract {

    private final Gson gson = new Gson();
    private static final String MED_PREFIX = "MEDICINE_";
    private static final String PHARMA_PREFIX = "PHARMA_";

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public Medicine createMedicine(Context ctx, String medicineJson) {
        ChaincodeStub stub = ctx.getStub();
        Medicine medicine = gson.fromJson(medicineJson, Medicine.class);
        String key = MED_PREFIX + medicine.getId();

        if (medicine.getId() == null || medicine.getId().isEmpty()) {
            throw new ChaincodeException("Medicine ID cannot be empty");
        }
        if (!stub.getStringState(key).isEmpty()) {
            throw new ChaincodeException("Medicine already exists: " + medicine.getId());
        }

        stub.putStringState(key, gson.toJson(medicine));
        return medicine;
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public Medicine readMedicine(Context ctx, String medicineId) {
        String key = MED_PREFIX + medicineId;
        String json = ctx.getStub().getStringState(key);
        if (json == null || json.isEmpty()) {
            throw new ChaincodeException("Medicine not found: " + medicineId);
        }
        return gson.fromJson(json, Medicine.class);
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public List<Medicine> getAllMedicines(Context ctx) {
        ChaincodeStub stub = ctx.getStub();
        List<Medicine> medicines = new ArrayList<>();

        try (QueryResultsIterator<KeyValue> results = stub.getStateByRange(MED_PREFIX, MED_PREFIX + "\uFFFF")) {
            for (KeyValue kv : results) {
                try {
                    Medicine med = gson.fromJson(kv.getStringValue(), Medicine.class);
                    if (med != null && med.getId() != null) {
                        medicines.add(med);
                    }
                } catch (JsonSyntaxException ignored) {}
            }
        } catch (Exception e) {
            throw new ChaincodeException(e.getMessage());
        }
        return medicines;
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public Medicine updateMedicine(Context ctx, String medicineJson) {
        ChaincodeStub stub = ctx.getStub();
        Medicine medicine = gson.fromJson(medicineJson, Medicine.class);
        String key = MED_PREFIX + medicine.getId();

        if (medicine.getId() == null || medicine.getId().isEmpty()) {
            throw new ChaincodeException("Medicine ID cannot be empty");
        }
        String existing = stub.getStringState(key);
        if (existing == null || existing.isEmpty()) {
            throw new ChaincodeException("Medicine not found: " + medicine.getId());
        }

        stub.putStringState(key, gson.toJson(medicine));
        return medicine;
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public String deleteMedicine(Context ctx, String medicineId) {
        ChaincodeStub stub = ctx.getStub();
        String key = MED_PREFIX + medicineId;
        String existing = stub.getStringState(key);

        if (existing == null || existing.isEmpty()) {
            throw new ChaincodeException("Medicine not found: " + medicineId);
        }

        try (QueryResultsIterator<KeyValue> results = stub.getStateByRange(PHARMA_PREFIX, PHARMA_PREFIX + "\uFFFF")) {
            for (KeyValue kv : results) {
                try {
                    Pharma pharma = gson.fromJson(kv.getStringValue(), Pharma.class);
                    if (pharma != null && pharma.getMedicineIds() != null &&
                            pharma.getMedicineIds().contains(medicineId)) {
                        pharma.getMedicineIds().remove(medicineId);
                        stub.putStringState(PHARMA_PREFIX + pharma.getPharmaId(), gson.toJson(pharma));
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            throw new ChaincodeException(e.getMessage());
        }

        stub.delState(key);
        return "Deleted Medicine " + medicineId;
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public List<Medicine> searchMedicineByName(Context ctx, String name) throws Exception {
        ChaincodeStub stub = ctx.getStub();
        List<Medicine> found = new ArrayList<>();

        try (QueryResultsIterator<KeyValue> results = stub.getStateByRange(MED_PREFIX, MED_PREFIX + "\uFFFF")) {
            for (KeyValue kv : results) {
                try {
                    Medicine med = gson.fromJson(kv.getStringValue(), Medicine.class);
                    if (med != null && med.getName() != null &&
                            med.getName().equalsIgnoreCase(name)) {
                        found.add(med);
                    }
                } catch (Exception ignored) {}
            }
        }
        if (found.isEmpty()) {
            throw new ChaincodeException("No medicine found with name: " + name);
        }
        return found;
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public Medicine updateMedicineStock(Context ctx, String medicineId, int newStock) {
        ChaincodeStub stub = ctx.getStub();
        String key = MED_PREFIX + medicineId;
        String json = stub.getStringState(key);

        if (json == null || json.isEmpty()) {
            throw new ChaincodeException("Medicine not found: " + medicineId);
        }

        Medicine med = gson.fromJson(json, Medicine.class);
        med.setStock(newStock);
        stub.putStringState(key, gson.toJson(med));
        return med;
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public Pharma addMedicineToPharma(Context ctx, String pharmaId, String medicineId) {
        ChaincodeStub stub = ctx.getStub();

        String pharmaKey = PHARMA_PREFIX + pharmaId;
        String medKey = MED_PREFIX + medicineId;

        String pharmaJson = stub.getStringState(pharmaKey);
        if (pharmaJson == null || pharmaJson.isEmpty()) {
            throw new ChaincodeException("Pharma not found: " + pharmaId);
        }

        String medJson = stub.getStringState(medKey);
        if (medJson == null || medJson.isEmpty()) {
            throw new ChaincodeException("Medicine not found: " + medicineId);
        }

        Pharma pharma = gson.fromJson(pharmaJson, Pharma.class);
        if (!pharma.getMedicineIds().contains(medicineId)) {
            pharma.getMedicineIds().add(medicineId);
        }

        stub.putStringState(pharmaKey, gson.toJson(pharma));
        return pharma;
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public Pharma removeMedicineFromPharma(Context ctx, String pharmaId, String medicineId) {
        ChaincodeStub stub = ctx.getStub();
        String pharmaKey = PHARMA_PREFIX + pharmaId;
        String pharmaJson = stub.getStringState(pharmaKey);

        if (pharmaJson == null || pharmaJson.isEmpty()) {
            throw new ChaincodeException("Pharma not found: " + pharmaId);
        }

        Pharma pharma = gson.fromJson(pharmaJson, Pharma.class);
        pharma.getMedicineIds().remove(medicineId);
        stub.putStringState(pharmaKey, gson.toJson(pharma));
        return pharma;
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public List<Medicine> getMedicinesByPharma(Context ctx, String pharmaId) {
        ChaincodeStub stub = ctx.getStub();
        String pharmaKey = PHARMA_PREFIX + pharmaId;
        String pharmaJson = stub.getStringState(pharmaKey);

        if (pharmaJson == null || pharmaJson.isEmpty()) {
            throw new ChaincodeException("Pharma not found: " + pharmaId);
        }

        Pharma pharma = gson.fromJson(pharmaJson, Pharma.class);
        List<Medicine> medicines = new ArrayList<>();

        for (String medId : pharma.getMedicineIds()) {
            String medJson = stub.getStringState(MED_PREFIX + medId);
            if (medJson != null && !medJson.isEmpty()) {
                medicines.add(gson.fromJson(medJson, Medicine.class));
            }
        }

        return medicines;
    }
}
