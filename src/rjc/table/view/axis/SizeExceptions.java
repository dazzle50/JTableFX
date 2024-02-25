/**************************************************************************
 *  Copyright (C) 2024 by Richard Crook                                   *
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

import java.util.HashMap;
import java.util.Map;

/*************************************************************************************************/
/***************************** Table axis exceptions to default size *****************************/
/*************************************************************************************************/

public class SizeExceptions
{
  // maps view index to nominal size
  private Map<Integer, Integer> m_exceptions = new HashMap<>();

  /******************************************** clear ********************************************/
  public void clear()
  {
    // empty the exceptions map
    m_exceptions.clear();
  }

  /****************************************** trimCount ******************************************/
  public void trimCount( int newCount )
  {
    // remove any exceptions above new count
    for ( int key : m_exceptions.keySet() )
      if ( key >= newCount )
        m_exceptions.remove( key );
  }

  /**************************************** setMinimumSize ***************************************/
  public void setMinimumSize( int newMinSize )
  {
    // update exceptions to ensure they are all new minimum size or larger
    for ( var exception : m_exceptions.entrySet() )
      if ( exception.getValue() < newMinSize )
        exception.setValue( newMinSize );
  }

  /******************************************* setSize *******************************************/
  public int setSize( int index, int newSize, int defaultSize )
  {
    // create a size exception (even if same as default) and return old size
    int oldSize = m_exceptions.getOrDefault( index, defaultSize );
    m_exceptions.put( index, newSize );
    return oldSize;
  }

  /*************************************** getSizeOrDefault **************************************/
  public int getSizeOrDefault( int index, int defaultSize )
  {
    // get nominal-size exception or default
    return m_exceptions.getOrDefault( index, defaultSize );
  }

  /*************************************** removeException ***************************************/
  public void remove( int index )
  {
    // empty the exceptions map
    m_exceptions.remove( index );
  }

}
