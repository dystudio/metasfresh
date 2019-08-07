package de.metas.invoicecandidate.internalbusinesslogic;

import static de.metas.util.lang.CoalesceUtil.coalesce;
import static org.adempiere.model.InterfaceWrapperHelper.create;
import static org.adempiere.model.InterfaceWrapperHelper.isNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.adempiere.exceptions.AdempiereException;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

import de.metas.inout.model.I_M_InOutLine;
import de.metas.inoutcandidate.api.IInOutCandidateBL;
import de.metas.inoutcandidate.spi.impl.ReceiptQty;
import de.metas.invoicecandidate.InvoiceCandidateId;
import de.metas.invoicecandidate.api.IInvoiceCandDAO;
import de.metas.invoicecandidate.internalbusinesslogic.DeliveredData.DeliveredDataBuilder;
import de.metas.invoicecandidate.internalbusinesslogic.ShipmentData.ShipmentDataBuilder;
import de.metas.invoicecandidate.internalbusinesslogic.ShippedQtyItem.ShippedQtyItemBuilder;
import de.metas.invoicecandidate.model.I_C_InvoiceCandidate_InOutLine;
import de.metas.lang.SOTrx;
import de.metas.product.ProductId;
import de.metas.quantity.Quantity;
import de.metas.quantity.Quantitys;
import de.metas.quantity.StockQtyAndUOMQty;
import de.metas.quantity.StockQtyAndUOMQtys;
import de.metas.uom.UOMConversionContext;
import de.metas.uom.UomId;
import de.metas.util.Services;
import de.metas.util.lang.Percent;
import lombok.NonNull;
import lombok.Value;

/*
 * #%L
 * de.metas.swat.base
 * %%
 * Copyright (C) 2019 metas GmbH
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
@lombok.Builder
public class DeliveredDataLoader
{
	@NonNull
	UomId stockUomId;

	@NonNull
	UomId icUomId;

	@NonNull
	ProductId productId;

	@NonNull
	InvoiceCandidateId invoiceCandidateId;

	@NonNull
	SOTrx soTrx;

	@NonNull // always empty, if soTrx, sometimes set if poTrx
	Optional<Percent> deliveryQualityDiscount;

	DeliveredData loadDeliveredQtys()
	{
		final DeliveredDataBuilder result = DeliveredData.builder();

		final IInvoiceCandDAO invoiceCandDAO = Services.get(IInvoiceCandDAO.class);
		final List<I_C_InvoiceCandidate_InOutLine> icIolAssociationRecords = invoiceCandDAO.retrieveICIOLAssociationsExclRE(invoiceCandidateId);

		if (soTrx.isPurchase())
		{
			result.receiptData(loadReceiptQualityData(icIolAssociationRecords));
		}
		if (soTrx.isSales())
		{
			result.shipmentData(loadShipmentData(icIolAssociationRecords));
		}
		return result.build();
	}

	private ShipmentData loadShipmentData(@NonNull final List<I_C_InvoiceCandidate_InOutLine> icIolAssociationRecords)
	{
		final ImmutableList<ShippedQtyItem> shippedQtyItems = loadshippedQtyItems(icIolAssociationRecords);

		Quantity qtyInStockUom = Quantitys.createZero(stockUomId);
		Quantity qtyNominal = Quantitys.createZero(icUomId);
		Quantity qtyCatch = Quantitys.createZero(icUomId);

		final ShipmentDataBuilder result = ShipmentData.builder()
				.productId(productId)
				.shippedQtyItems(shippedQtyItems);

		final UOMConversionContext conversionCtx = UOMConversionContext.of(productId);

		final ArrayList<ShippedQtyItem> deliveredQtyItemsWithCatch = new ArrayList<ShippedQtyItem>();
		final ArrayList<ShippedQtyItem> deliveredQtyItemsWithoutCatch = new ArrayList<ShippedQtyItem>();
		for (final ShippedQtyItem deliveredQtyItem : shippedQtyItems)
		{
			qtyInStockUom = Quantitys.add(conversionCtx,
					qtyInStockUom,
					deliveredQtyItem.getQtyInStockUom());

			qtyNominal = Quantitys.add(conversionCtx,
					qtyNominal,
					coalesce(deliveredQtyItem.getQtyOverride(), deliveredQtyItem.getQtyNominal()));

			final Quantity qtyCatchEffective = coalesce(
					deliveredQtyItem.getQtyOverride(),
					deliveredQtyItem.getQtyCatch());
			if (qtyCatchEffective == null)
			{
				deliveredQtyItemsWithoutCatch.add(deliveredQtyItem);
			}
			else
			{
				deliveredQtyItemsWithCatch.add(deliveredQtyItem);
				qtyCatch = Quantitys.add(conversionCtx,
						qtyCatch,
						qtyCatchEffective);
			}
		}

		boolean hasItemsWithoutCatch = !deliveredQtyItemsWithoutCatch.isEmpty();
		boolean hasItemsWithCatch = !deliveredQtyItemsWithCatch.isEmpty();

		if (hasItemsWithCatch && hasItemsWithoutCatch)
		{

			throw new AdempiereException("Either all or none if the invoice candidate's deliveredQtyItemsWithoutCatch nned to have a catch quantity") // can be fixed by setting QtyCatchOverride
					.appendParametersToMessage()
					.setParameter("invoiceCandidateId", invoiceCandidateId)
					.setParameter("itemsWithCatch", ImmutableList.copyOf(deliveredQtyItemsWithCatch))
					.setParameter("itemsWithoutCatch", ImmutableList.copyOf(deliveredQtyItemsWithoutCatch));
		}

		result.qtyInStockUom(qtyInStockUom)
				.qtyNominal(qtyNominal);

		if (hasItemsWithCatch)
		{
			result.qtyCatch(qtyCatch);
		}

		final ShipmentData build = result.build();
		return build;
	}

	private ReceiptQualityData loadReceiptQualityData(@NonNull final List<I_C_InvoiceCandidate_InOutLine> icIolAssociationRecords)
	{
		final IInOutCandidateBL inOutCandidateBL = Services.get(IInOutCandidateBL.class);

		StockQtyAndUOMQty qtysWithIssues = StockQtyAndUOMQtys.createZero(productId, icUomId);
		StockQtyAndUOMQty qtysTotal = StockQtyAndUOMQtys.createZero(productId, icUomId);

		final ReceiptQty receiptQtys = new ReceiptQty();
		for (final I_C_InvoiceCandidate_InOutLine iciol : icIolAssociationRecords)
		{
			final I_M_InOutLine inoutLine = create(iciol.getM_InOutLine(), I_M_InOutLine.class);

			final StockQtyAndUOMQty qtys = StockQtyAndUOMQtys.create(productId, iciol.getQtyDelivered(),
					UomId.ofRepoId(iciol.getC_UOM_ID()), iciol.getQtyDeliveredInUOM_Nominal());

			qtysTotal = StockQtyAndUOMQtys.add(qtysTotal, qtys);
			if (inoutLine.isInDispute())
			{
				qtysWithIssues = StockQtyAndUOMQtys.add(qtysWithIssues, qtys);
			}

		}

		return ReceiptQualityData.builder()
				.qtysTotal(qtysTotal)
				.qtysWithIssues(qtysWithIssues)
				.build();
	}

	private ImmutableList<ShippedQtyItem> loadshippedQtyItems(@NonNull final List<I_C_InvoiceCandidate_InOutLine> icIolAssociationRecords)
	{
		final Builder<ShippedQtyItem> result = ImmutableList.builder();

		for (final I_C_InvoiceCandidate_InOutLine icIolAssociationRecord : icIolAssociationRecords)
		{
			final ShippedQtyItemBuilder deliveredQtyItem = ShippedQtyItem.builder();

			final Quantity qtyInStockUom = Quantitys.create(
					icIolAssociationRecord.getM_InOutLine().getMovementQty(),
					stockUomId);
			deliveredQtyItem.qtyInStockUom(qtyInStockUom);

			final UomId deliveryUomId = UomId.ofRepoId(icIolAssociationRecord.getC_UOM_ID());
			final Quantity qtyNominal = Quantitys.create(
					icIolAssociationRecord.getQtyDeliveredInUOM_Nominal(),
					deliveryUomId);
			deliveredQtyItem.qtyNominal(qtyNominal);

			if (!isNull(icIolAssociationRecord, I_C_InvoiceCandidate_InOutLine.COLUMNNAME_QtyDeliveredInUOM_Catch))
			{
				final Quantity qtyCatch = Quantitys.create(
						icIolAssociationRecord.getQtyDeliveredInUOM_Catch(),
						deliveryUomId);
				deliveredQtyItem.qtyCatch(qtyCatch);
			}

			if (!isNull(icIolAssociationRecord, I_C_InvoiceCandidate_InOutLine.COLUMNNAME_QtyDeliveredInUOM_Override))
			{
				final Quantity qtyOverride = Quantitys.create(
						icIolAssociationRecord.getQtyDeliveredInUOM_Override(),
						deliveryUomId);
				deliveredQtyItem.qtyOverride(qtyOverride);
			}
			result.add(deliveredQtyItem.build());
		}

		return result.build();
	}

}
