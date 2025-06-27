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

package rjc.table.control;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;

import javafx.scene.input.KeyEvent;
import rjc.table.Utils;
import rjc.table.control.dropdown.ChooseDropDown;

/*************************************************************************************************/
/********************** Control for choosing a value from a drop-down list ***********************/
/*************************************************************************************************/

public class ChooseField extends ButtonField
{
  private ChooseDropDown    m_dropdown; // choice drop-down
  private Object[]          m_choices;  // array of objects choice to be from
  private ArrayList<String> m_text;     // list of value string equivalents
  private int               m_index;    // index of current selected item

  /**************************************** constructor ******************************************/
  public ChooseField( Object[] choices )
  {
    // initiate the field
    setEditable( false );
    setButtonType( ButtonType.DOWN );
    m_dropdown = new ChooseDropDown( this );
    setChoices( choices );

    // react to key presses and button mouse clicks
    setOnKeyPressed( event -> keyPressed( event ) );
    setOnKeyTyped( event -> findNext( event.getCharacter() ) );
  }

  /**************************************** constructor ******************************************/
  public ChooseField( List<Object> choices )
  {
    // convert list to array for choices
    this( choices.toArray() );
  }

  /***************************************** setChoices ******************************************/
  public void setChoices( Object[] choices )
  {
    // get current choice before updating choices
    String currentChoice = m_text == null ? "" : getText( getSelectedIndex() );

    // generate string equivalent of choice objects
    m_choices = choices;
    m_text = new ArrayList<>( choices.length );
    for ( int index = 0; index < choices.length; index++ )
      m_text.add( choices[index] == null ? "null" : choices[index].toString() );

    // try to find current-choice
    int index = m_text.indexOf( currentChoice );
    setSelectedIndex( index < 0 ? 0 : index );
  }

  /***************************************** setChoices ******************************************/
  public void setChoices( List<Object> choices )
  {
    // convert list to array for updated choices
    setChoices( choices.toArray() );
  }

  /***************************************** changeValue *****************************************/
  @Override
  public void changeValue( double delta )
  {
    // change selected index
    setSelectedIndex( m_index - (int) delta );
  }

  /****************************************** getCount *******************************************/
  public int getCount()
  {
    // return number of items user can choose from
    return m_choices.length;
  }

  /************************************** getSelectedIndex ***************************************/
  public int getSelectedIndex()
  {
    // return currently selected index
    return m_index;
  }

  /************************************** setSelectedIndex ***************************************/
  public void setSelectedIndex( int index )
  {
    // set selected index and update displayed text to match
    index = index % m_choices.length;
    m_index = index < 0 ? index + m_choices.length : index;

    setText( getText( m_index ) );
    m_dropdown.scrollToIndex( m_index );
  }

  /******************************************* getText *******************************************/
  public String getText( int index )
  {
    // return text equivalent of object at specified index
    return m_text.get( index );
  }

  /***************************************** getSelected *****************************************/
  public Object getSelected()
  {
    // return currently selected object
    return m_choices[m_index];
  }

  /***************************************** setSelected *****************************************/
  public void setSelected( Object value )
  {
    // set currently selected object
    for ( int index = 0; index < m_choices.length; index++ )
      if ( m_choices[index] == value )
      {
        setSelectedIndex( index );
        return;
      }

    // if string try to find choice that starts with this string
    if ( value instanceof String text )
    {
      findNext( text );
      return;
    }

    // if index not found and value is null, then set index to zero
    if ( value == null )
    {
      setSelectedIndex( 0 );
      return;
    }

    // value not found in list so throw exception
    throw new InvalidParameterException( Utils.objectsString( value ).toString() );
  }

  /***************************************** keyPressed ******************************************/
  @Override
  protected void keyPressed( KeyEvent event )
  {
    // action key press to change current selected item
    event.consume();
    switch ( event.getCode() )
    {
      case DOWN:
      case KP_DOWN:
      case RIGHT:
      case KP_RIGHT:
      case PAGE_DOWN:
        changeValue( -1 );
        break;
      case UP:
      case KP_UP:
      case LEFT:
      case KP_LEFT:
      case PAGE_UP:
        changeValue( 1 );
        break;
      case HOME:
        setSelectedIndex( 0 );
        break;
      case END:
        setSelectedIndex( getCount() - 1 );
        break;
      case F2:
        m_dropdown.toggle();
        break;
      default:
        break;
    }
  }

  /****************************************** findNext *******************************************/
  public void findNext( String start )
  {
    // find next item that starts with specified string (case-insensitive)
    start = start.toLowerCase();
    for ( int delta = 1; delta < m_choices.length; delta++ )
    {
      int index = ( m_index + delta ) % m_choices.length;
      if ( m_text.get( index ).toLowerCase().startsWith( start ) )
      {
        setSelectedIndex( index );
        return;
      }
    }
  }

}
