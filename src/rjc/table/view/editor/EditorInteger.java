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

package rjc.table.view.editor;

import rjc.table.control.NumberSpinField;

/*************************************************************************************************/
/****************************** Table cell spin editor for integer *******************************/
/*************************************************************************************************/

public class EditorInteger extends AbstractCellEditor
{
  private NumberSpinField m_spin = new NumberSpinField();

  /**************************************** constructor ******************************************/
  public EditorInteger()
  {
    // create spin table cell editor for integer
    super();
    setControl( m_spin );
  }

  /******************************************* getValue ******************************************/
  @Override
  public Object getValue()
  {
    // get editor integer value
    return m_spin.getInteger();
  }

  /******************************************* setValue ******************************************/
  @Override
  public void setValue( Object value )
  {
    // set spin field value
    m_spin.setValue( value );
  }

  /****************************************** setFormat ******************************************/
  public void setFormat( String format, int maxIntegerDigits )
  {
    // set number format
    m_spin.setFormat( format, maxIntegerDigits, 0 );
  }

  /******************************************* setRange ******************************************/
  public void setRange( int minValue, int maxValue )
  {
    // set range
    m_spin.setRange( minValue, maxValue );
  }

  /***************************************** setStepPage *****************************************/
  public void setStepPage( int step, int page )
  {
    // set step and page increment/decrement sizes
    m_spin.setStepPage( step, page );
  }

  /*************************************** setPrefixSuffix ***************************************/
  public void setPrefixSuffix( String prefix, String suffix )
  {
    // set prefix and suffix
    m_spin.setPrefixSuffix( prefix, suffix );
  }

}