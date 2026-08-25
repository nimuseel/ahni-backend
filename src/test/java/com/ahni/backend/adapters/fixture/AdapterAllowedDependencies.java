package com.ahni.backend.adapters.fixture;

import com.ahni.backend.domain.fixture.DomainFixture;
import com.ahni.backend.ports.fixture.PortFixture;

public class AdapterAllowedDependencies {

	private final DomainFixture domain = new DomainFixture();
	private final PortFixture port = new PortFixture();
}
