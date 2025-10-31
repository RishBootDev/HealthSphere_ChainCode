package org.rishbootdev.chaincode.model;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import java.util.List;

@DataType
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Lab {

    @Property
    private String labId;
    @Property private String name;
    @Property private List<String> reportIds;

}

