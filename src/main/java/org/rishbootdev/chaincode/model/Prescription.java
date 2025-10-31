package org.rishbootdev.chaincode.model;


import lombok.Data;
import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import java.util.List;

@DataType
@Data
public class Prescription {

    @Property private String prescriptionId;
    @Property private String patientId;
    @Property private String doctorId;
    @Property private List<String> medicineIdList;
    @Property private String issuedDate;
    @Property private String remarks;

}
