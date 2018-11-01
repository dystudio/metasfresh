package de.metas.costing;

import org.adempiere.acct.api.AcctSchemaId;
import org.adempiere.exceptions.AdempiereException;
import org.adempiere.mm.attributes.AttributeSetInstanceId;
import org.adempiere.service.ClientId;
import org.adempiere.service.OrgId;

import de.metas.product.ProductId;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/*
 * #%L
 * de.metas.business
 * %%
 * Copyright (C) 2018 metas GmbH
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program. If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

@Value
public class CostSegment
{
	AcctSchemaId acctSchemaId;
	CostTypeId costTypeId;
	CostingLevel costingLevel;
	
	ClientId clientId;
	OrgId orgId;
	ProductId productId;
	AttributeSetInstanceId attributeSetInstanceId;

	@Builder(toBuilder = true)
	private CostSegment(
			@NonNull final CostingLevel costingLevel,
			@NonNull final AcctSchemaId acctSchemaId,
			@NonNull final CostTypeId costTypeId,
			@NonNull final ClientId clientId,
			@NonNull final OrgId orgId,
			@NonNull final ProductId productId,
			final AttributeSetInstanceId attributeSetInstanceId)
	{
		this.costingLevel = costingLevel;
		this.acctSchemaId = acctSchemaId;
		this.costTypeId = costTypeId;
		this.productId = productId;
		this.clientId = clientId;

		if (costingLevel == CostingLevel.Client)
		{
			this.orgId = OrgId.ANY;
			this.attributeSetInstanceId = AttributeSetInstanceId.NONE;
		}
		else if (costingLevel == CostingLevel.Organization)
		{
			this.orgId = orgId;
			this.attributeSetInstanceId = AttributeSetInstanceId.NONE;
		}
		else if (costingLevel == CostingLevel.BatchLot)
		{
			this.orgId = OrgId.ANY;
			this.attributeSetInstanceId = attributeSetInstanceId;
		}
		else
		{
			throw new AdempiereException("Unknown costingLevel: " + costingLevel);
		}
	}
}
