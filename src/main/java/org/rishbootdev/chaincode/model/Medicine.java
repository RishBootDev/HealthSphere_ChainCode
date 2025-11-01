package org.rishbootdev.chaincode.model;

import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

@DataType
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Medicine {

    @Property
    @SerializedName("name")
    private String name;

    @Property
    @SerializedName("id")
    private String id;

    @Property
    @SerializedName("manufacturer")
    private String manufacturer;

    @Property
    @SerializedName("dosage")
    private String dosage;

    @Property
    @SerializedName("stock")
    private int stock;

    @Property
    @SerializedName("expiryDate")
    private String expiryDate;
}
