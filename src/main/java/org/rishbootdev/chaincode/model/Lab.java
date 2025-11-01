package org.rishbootdev.chaincode.model;


import com.google.gson.annotations.SerializedName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;
import org.hyperledger.fabric.contract.annotation.Serializer;

import java.util.ArrayList;
import java.util.List;

@DataType
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Lab {

    @Property
    @SerializedName("labId")
    private String labId;

    @Property
    @SerializedName("name")
    private String name;
    @Property
    @SerializedName("reportIds")
    private List<String> reportIds=new ArrayList<>();

}

