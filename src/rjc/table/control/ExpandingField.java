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

package rjc.table.control;

import java.util.regex.Pattern;

import javafx.scene.control.TextField;
import javafx.scene.text.Text;
import rjc.table.Utils;
import rjc.table.signal.ObservableStatus;

/*************************************************************************************************/
/************* Enhanced JavaFX TextField that can expand and only allows valid text **************/
/*************************************************************************************************/

public class ExpandingField extends TextField implements IHasObservableStatus
{
  private Pattern          m_allowed;  // pattern defining text allowed to be entered
  private double           m_minWidth; // minimum width for editor in pixels
  private double           m_maxWidth; // maximum width for editor in pixels

  private ObservableStatus m_status;   // error status of text field

  /**************************************** constructor ******************************************/
  public ExpandingField()
  {
    // listen to text changes to check if field width needs changing
    textProperty().addListener( ( observable, oldText, newText ) ->
    {
      // if min & max width set, increase editor width if needed to show whole text
      if ( m_minWidth > 0.0 && m_maxWidth > m_minWidth )
      {
        Text text = new Text( newText );
        text.setFont( getFont() );
        double width = text.getLayoutBounds().getWidth() + getPadding().getLeft() + getPadding().getRight() + 2;
        width = Utils.clamp( width, m_minWidth, m_maxWidth );
        if ( getWidth() != width )
        {
          setMinWidth( width );
          setMaxWidth( width );
        }
      }
    } );
  }

  /***************************************** replaceText *****************************************/
  @Override
  public void replaceText( int start, int end, String text )
  {
    // only progress text updates that result in 'allowed' new text
    String oldText = getText();
    String newText = oldText.substring( 0, start ) + text + oldText.substring( end );

    if ( isAllowed( newText ) )
      super.replaceText( start, end, text );
  }

  /****************************************** setAllowed *****************************************/
  public void setAllowed( String regex )
  {
    // regular expression that limits what can be entered into editor
    m_allowed = regex == null ? null : Pattern.compile( regex );
  }

  /****************************************** isAllowed ******************************************/
  public boolean isAllowed( String text )
  {
    // return true if text is allowed
    return m_allowed == null || m_allowed.matcher( text ).matches();
  }

  /****************************************** setWidths ******************************************/
  public void setWidths( double min, double max )
  {
    // set editor minimum and maximum width (only used for table cell editors)
    m_minWidth = min;
    m_maxWidth = max;
  }

  /***************************************** setStatus *******************************************/
  @Override
  public void setStatus( ObservableStatus status )
  {
    // set text field status
    if ( status == null )
      throw new NullPointerException( "Status cannot be null" );
    m_status = status;
  }

  /***************************************** getStatus *******************************************/
  @Override
  public ObservableStatus getStatus()
  {
    // return text field status
    return m_status;
  }

}