package org.rishbootdev.chaincode.model;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@DataType
public class Pharma {

    @Property
    @SerializedName("name")
    private String name;

    @Property
    @SerializedName("pharmaId")
    private String pharmaId;

    @Property
    @SerializedName("medicineId")
    private List<String> medicineId=new ArrayList<>();
}
