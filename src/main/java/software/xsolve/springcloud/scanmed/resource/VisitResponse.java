package software.xsolve.springcloud.scanmed.resource;

import java.util.List;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class VisitResponse {

	private List<DoctorSlot> doctorSlots;

}

