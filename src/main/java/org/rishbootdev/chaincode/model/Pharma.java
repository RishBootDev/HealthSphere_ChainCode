package org.rishbootdev.chaincode.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hyperledger.fabric.contract.annotation.Property;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Pharma {

    @Property
    private String name;
    @Property
    private String pharmaId;
    @Property
    private List<String> medicineId;
}
