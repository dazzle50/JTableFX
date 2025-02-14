/**************************************************************************
 *  Copyright (C) 2025 by Richard Crook                                   *
 *  https://github.com/dazzle50/JTableFX                                  *
 *                                                                        *
 *  This program is free software: you can redistribute it and/or modify  *
 *  it under the terms of the GNU General Public License as published by  *
 *  the Free Software Foundation, either version 3 of the License, or     *
 *  (at your option) any later version.                                   *
 *                                                                        *
 *  This program is distributed in the hope that it will be useful,       *
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *  GNU General Public License for more details.                          *
 *                                                                        *
 *  You should have received a copy of the GNU General Public License     *
 *  along with this program.  If not, see http://www.gnu.org/licenses/    *
 **************************************************************************/

package rjc.table.view.axis;

import rjc.table.Utils;
import rjc.table.signal.ObservableInteger.ReadOnlyInteger;

/*************************************************************************************************/
/**************************** Table X or Y axis with movement support ****************************/
/*************************************************************************************************/

public class TableAxis extends AxisMap
{

  /**************************************** constructor ******************************************/
  public TableAxis( ReadOnlyInteger countProperty )
  {
    // pass count property to super class, and call reset
    super( countProperty );
    reset();
  }

  /****************************************** getVisible *******************************************/
  public int getVisible( int index )
  {
    // return index if visible, otherwise find a visible index
    if ( index < FIRSTCELL )
      return getFirstVisible();
    if ( index >= getCount() )
      return getLastVisible();
    if ( isIndexVisible( index ) )
      return index;

    return getNextVisible( index );
  }

  /************************************** getFirstVisible ****************************************/
  public int getFirstVisible()
  {
    // return first visible body cell index
    return getNextVisible( HEADER );
  }

  /*************************************** getLastVisible ****************************************/
  public int getLastVisible()
  {
    // return last visible body cell index
    return getPreviousVisible( getCount() );
  }

  /*************************************** getNextVisible ****************************************/
  public int getNextVisible( int index )
  {
    // return next visible body cell index, or last if there isn't one
    if ( index < HEADER )
      index = HEADER;

    int max = getCount();
    for ( int check = index + 1; check < max; check++ )
      if ( isIndexVisible( check ) )
        return check;
    for ( int check = index; check > HEADER; check-- )
      if ( isIndexVisible( check ) )
        return check;

    return INVALID;
  }

  /************************************* getPreviousVisible **************************************/
  public int getPreviousVisible( int index )
  {
    // return previous visible body cell index, or first if there isn't one
    int max = getCount();
    if ( index > max )
      index = max;

    for ( int check = index - 1; check > HEADER; check-- )
      if ( isIndexVisible( check ) )
        return check;
    for ( int check = index; check < max; check++ )
      if ( isIndexVisible( check ) )
        return check;

    return INVALID;
  }

  /****************************************** toString *******************************************/
  @Override
  public String toString()
  {
    // return as string
    return Utils.name( this ) + "[count=" + getCount() + "]";
  }
}
