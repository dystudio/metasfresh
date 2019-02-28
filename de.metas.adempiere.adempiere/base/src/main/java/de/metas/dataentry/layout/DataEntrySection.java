package de.metas.dataentry.layout;

import java.util.List;

import de.metas.dataentry.DataEntrySectionId;
import de.metas.i18n.ITranslatableString;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.Value;

/*
 * #%L
 * de.metas.adempiere.adempiere.base
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
public class DataEntrySection
{
//	public static DataEntrySectionBuilder newDefaultSectionBuilder()
//	{
//		return DataEntrySection.builder()
//				.id(DataEntrySectionId.DEFAULT)
//				.description(ITranslatableString.empty())
//				.caption(ITranslatableString.empty())
//				.internalName("DEFAULT");
//	}

	DataEntrySectionId id;

	ITranslatableString caption;
	ITranslatableString description;

	String internalName;

	boolean initallyClosed;

	List<DataEntryLine> dataEntryLines;

	@Builder
	private DataEntrySection(
			@NonNull final DataEntrySectionId id,
			@NonNull final ITranslatableString caption,
			@NonNull final ITranslatableString description,
			@NonNull final String internalName,
			final boolean initallyClosed,
			@Singular final List<DataEntryLine> dataEntryLines)
	{
		this.id = id;
		this.caption = caption;
		this.description = description;
		this.internalName = internalName;
		this.initallyClosed = initallyClosed;
		this.dataEntryLines = dataEntryLines;
	}
}
