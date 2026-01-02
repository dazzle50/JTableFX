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

package rjc.table.view.action;

import java.util.ArrayList;

import rjc.table.data.types.Date;
import rjc.table.data.types.DateTime;
import rjc.table.data.types.Time;
import rjc.table.view.action.Sort.SortType;

/*************************************************************************************************/
/****************************** Sortable list of table cell values *******************************/
/*************************************************************************************************/

public class SortList extends ArrayList<SortList.SortValue>
{
  private static final long serialVersionUID = 1L;

  // record to hold sortable value
  public record SortValue( int index, String text, int priority, double num )
  {
  }

  /******************************************* append ********************************************/
  public boolean append( int index, Object obj )
  {
    // append new sort-value to list based on object type
    if ( obj == null )
      return add( new SortValue( index, null, 3 * Integer.MAX_VALUE, 0.0 ) );

    if ( obj instanceof String text )
      return add( new SortValue( index, text, 0, 0.0 ) );

    if ( obj instanceof Number num )
      return add( new SortValue( index, null, 1, num.doubleValue() ) );

    if ( obj instanceof Time time )
      return add( new SortValue( index, null, 2, time.toMillisecondsOfDay() / (double) Time.MILLIS_PER_DAY ) );

    if ( obj instanceof Date date )
      return add( new SortValue( index, null, 3, date.getEpochDay() ) );

    if ( obj instanceof DateTime dt )
      return add( new SortValue( index, null, 4, dt.toMilliseconds() / (double) Time.MILLIS_PER_DAY ) );

    if ( obj instanceof Enum enm )
      return add( new SortValue( index, null, 5 + Math.abs( enm.getClass().hashCode() ), enm.ordinal() ) );

    // for other object types, use identity hash code to provide unique but consistent number
    return add( new SortValue( index, null, 6 + Integer.MAX_VALUE + Math.abs( obj.getClass().hashCode() ),
        System.identityHashCode( obj ) ) );
  }

  /******************************************** sort *********************************************/
  public void sort( SortType type )
  {
    // sort list based on priority, then text or number
    super.sort( ( a, b ) ->
    {
      int result;
      if ( a.priority != b.priority )
        result = Integer.compare( a.priority, b.priority );
      else if ( a.text != null && b.text != null )
        result = a.text.compareTo( b.text );
      else
        result = Double.compare( a.num, b.num );

      return type == SortType.ASCENDING ? result : -result;
    } );
  }

  /***************************************** getIndexes ******************************************/
  public int[] getIndexes()
  {
    // return array of indexes in current list order
    int[] indexes = new int[size()];
    for ( int i = 0; i < size(); i++ )
      indexes[i] = get( i ).index;

    return indexes;
  }

}
