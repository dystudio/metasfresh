package de.metas.purchasecandidate.purchaseordercreation.remotepurchaseitem;

import static org.adempiere.model.InterfaceWrapperHelper.newInstanceOutOfTrx;
import static org.adempiere.model.InterfaceWrapperHelper.save;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.adempiere.test.AdempiereTestHelper;
import org.adempiere.util.lang.impl.TableRecordReference;
import org.compiere.model.I_C_UOM;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.metas.purchasecandidate.PurchaseCandidate;
import de.metas.purchasecandidate.PurchaseCandidateTestTool;
import de.metas.quantity.Quantity;
import de.metas.util.time.SystemTime;

/*
 * #%L
 * de.metas.purchasecandidate.base
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

public class PurchaseOrderItemTest
{
	private I_C_UOM EACH;
	private Quantity ONE;
	private Quantity TEN;

	@BeforeEach
	public void init()
	{
		AdempiereTestHelper.get().init();

		this.EACH = createUOM("Ea");
		this.ONE = Quantity.of(BigDecimal.ONE, EACH);
		this.TEN = Quantity.of(BigDecimal.TEN, EACH);
	}

	private I_C_UOM createUOM(final String name)
	{
		final I_C_UOM uom = newInstanceOutOfTrx(I_C_UOM.class);
		uom.setName(name);
		uom.setUOMSymbol(name);
		save(uom);
		return uom;
	}

	@Test
	public void toString_without_StackOverflowError()
	{
		final PurchaseCandidate purchaseCandidate = PurchaseCandidateTestTool.createPurchaseCandidate(20, ONE);

		final PurchaseOrderItem purchaseOrderItem = purchaseCandidate.createOrderItem()
				.purchasedQty(TEN)
				.datePromised(SystemTime.asZonedDateTime())
				.transactionReference(TableRecordReference.of("sometable", 40))
				.remotePurchaseOrderId("remotePurchaseOrderId")
				.buildAndAddToParent();

		assertThat(purchaseOrderItem.toString()).isNotBlank();
	}

}
