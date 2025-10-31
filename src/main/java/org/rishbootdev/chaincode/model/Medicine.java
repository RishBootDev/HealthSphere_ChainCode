package org.rishbootdev.chaincode.model;

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
    private String name;
    @Property private String Id;
    @Property private String manufacturer;
    @Property private String dosage;
    @Property private int stock;
    @Property private String expiryDate;

}