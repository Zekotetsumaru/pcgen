/*
 * WieldCategory.java
 * Copyright (c) 2010 Tom Parker <thpr@users.sourceforge.net>
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 * @author Jayme Cox <jaymecox@users.sourceforge.net>
 * Created on November 21, 2003, 11:26 PM
 *
 * Current Ver: $Revision$
 * Last Editor: $Author$
 * Last Edited: $Date$
 *
 */
package pcgen.core.character;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import pcgen.cdom.base.Loadable;
import pcgen.cdom.enumeration.ObjectKey;
import pcgen.cdom.reference.CDOMSingleRef;
import pcgen.core.Equipment;
import pcgen.core.Globals;
import pcgen.core.PlayerCharacter;
import pcgen.core.QualifiedObject;
import pcgen.core.SizeAdjustment;
import pcgen.core.prereq.PrereqHandler;

public final class WieldCategory implements Loadable
{
	private URI sourceURI;
	private String categoryName;
	private int handsRequired;
	private boolean isFinessable;
	private int sizeDifference;
	private Map<Integer, Float> damageMultiplier = new HashMap<Integer, Float>();
	private Map<Integer, CDOMSingleRef<WieldCategory>> wcSteps = new HashMap<Integer, CDOMSingleRef<WieldCategory>>();
	private List<QualifiedObject<CDOMSingleRef<WieldCategory>>> categorySwitches = new ArrayList<QualifiedObject<CDOMSingleRef<WieldCategory>>>();

    @Override
	public URI getSourceURI()
	{
		return sourceURI;
	}

    @Override
	public void setSourceURI(URI source)
	{
		sourceURI = source;
	}

	public void setKeyName(String key)
	{
		setName(key);
	}

    @Override
	public String getKeyName()
	{
		return getDisplayName();
	}

    @Override
	public String getDisplayName()
	{
		return categoryName;
	}

    @Override
	public void setName(String name)
	{
		categoryName = name;
	}

    @Override
	public String getLSTformat()
	{
		return getDisplayName();
	}

    @Override
	public boolean isInternal()
	{
		return false;
	}

    @Override
	public boolean isType(String type)
	{
		return false;
	}

	public int getHandsRequired()
	{
		return handsRequired;
	}

	public void setHandsRequired(int hands)
	{
		handsRequired = hands;
	}

	public void setFinessable(boolean finessable)
	{
		isFinessable = finessable;
	}

	public boolean isFinessable()
	{
		return isFinessable;
	}

	public void setSizeDifference(int difference)
	{
		sizeDifference = difference;
	}

	public void setWieldCategoryStep(int location,
			CDOMSingleRef<WieldCategory> stepCat)
	{
		CDOMSingleRef<WieldCategory> previous = wcSteps.put(location, stepCat);
		if (previous != null)
		{
			// overwrite warning?
		}
	}

	public void addDamageMult(int numHands, float mult)
	{
		Float previous = damageMultiplier.put(numHands, mult);
		if (previous != null)
		{
			// overwrite warning?
		}
	}

	public WieldCategory getWieldCategoryStep(int steps)
	{
		CDOMSingleRef<WieldCategory> wcRef = wcSteps.get(steps);
		return wcRef == null ? null : wcRef.resolvesTo();
	}

	public void addCategorySwitch(
			QualifiedObject<CDOMSingleRef<WieldCategory>> qo)
	{
		categorySwitches.add(qo);
	}

	public int getObjectSizeInt(Equipment eq)
	{
		return eq.sizeInt() + sizeDifference;
	}

	/**
	 * Get the WieldCategory adjusted for the size difference between the weapon
	 * and the PC. This uses the 3.5 equipment sizes.
	 * 
	 * @param pc
	 *            Player character to get the weild category for.
	 * @param eq
	 *            Equipment to get the weild category for.
	 * @return The ajusted WieldCategory
	 */
	public WieldCategory adjustForSize(final PlayerCharacter pc,
			final Equipment eq)
	{
		if (pc == null || eq == null || eq.get(ObjectKey.WIELD) == null)
		{
			return this;
		}

		// Check if we have a bonus that changes the weapons effective size
		// for wield purposes.
		SizeAdjustment oldEqSa = eq.getSizeAdjustment();
		if (pc.sizeInt() != eq.sizeInt())
		{
			int aBump = 0;
			aBump += (int) pc.getTotalBonusTo("WIELDCATEGORY", eq
					.getWieldName());
			aBump += (int) pc.getTotalBonusTo("WIELDCATEGORY", "ALL");

			// loops for each equipment type
			int modWield = 0;
			for ( String eqType : eq.typeList() )
			{
				final StringBuilder sB = new StringBuilder("WEAPONPROF=TYPE.");
				sB.append(eqType);

				// get the type bonus (ex TYPE.MARTIAL)
				final int i = (int) pc.getTotalBonusTo(sB.toString(), "WIELDCATEGORY");

				// get the highest bonus
				if (i < modWield)
				{
					modWield = i;
				}
			}
			aBump += modWield;
						
			if (aBump != 0)
			{
				final int newSizeInt = eq.sizeInt() + aBump;
				final SizeAdjustment sadj = Globals.getContext().getReferenceContext()
						.getItemInOrder(SizeAdjustment.class, newSizeInt);
				eq.put(ObjectKey.SIZE, sadj);
			}
		}
		WieldCategory pcWCat = getSwitch(pc, eq);
		eq.put(ObjectKey.SIZE, oldEqSa);
		return pcWCat;
	}

	private WieldCategory getSwitch(PlayerCharacter pc, Equipment eq)
	{
		WieldCategory pcWCat = this;
		// TODO what if more than one matches??
		for (QualifiedObject<CDOMSingleRef<WieldCategory>> qo : categorySwitches)
		{
			if (PrereqHandler.passesAll(qo.getPrerequisiteList(), eq, pc))
			{
				pcWCat = qo.getRawObject().resolvesTo();
			}
		}
		return pcWCat;
	}
	
	@Override
	public int hashCode()
	{
		return categoryName.hashCode();
	}
	
	@Override
	public boolean equals(Object o)
	{
		if (o instanceof WieldCategory)
		{
			/*
			 * Light weight check due to ReferenceManufacturer enforcement
			 */
			WieldCategory other = (WieldCategory) o;
			return categoryName.equals(other.categoryName);
		}
		return false;
	}
}
