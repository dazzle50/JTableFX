/**************************************************************************
 *  Copyright (C) 2026 by Richard Crook                                   *
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
import java.util.Objects;

import javafx.scene.input.KeyEvent;
import rjc.table.Utils;
import rjc.table.control.dropdown.ChooseDropDown;

/*************************************************************************************************/
/********************** Control for choosing a value from a drop-down list ***********************/
/*************************************************************************************************/

/**
 * Non-editable field for selecting one object from a fixed, non-empty collection of choices.
 * <p>
 * Each choice is displayed using its {@link Object#toString()} value, with a {@code null} choice
 * displayed as the literal text {@code "null"}. Choices can be selected from the drop-down, stepped
 * cyclically with the keyboard or mouse wheel, or found by typing a case-insensitive prefix.
 * <p>
 * The supplied choice array or list is copied, so subsequent structural changes by the caller do
 * not affect this field. The choice objects themselves are not copied.
 */
public class ChooseField extends ButtonField
{
  private ChooseDropDown    m_dropdown; // choice drop-down
  private Object[]          m_choices;  // array of objects choice to be from
  private ArrayList<String> m_text;     // list of value string equivalents
  private int               m_index;    // index of current selected item

  /**************************************** constructor ******************************************/
  /**
   * Creates a field containing the supplied choices and initially selects the first choice.
   *
   * @param choices choices available for selection
   * @throws NullPointerException     if {@code choices} is null
   * @throws IllegalArgumentException if {@code choices} is empty
   */
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
  /**
   * Creates a field containing the supplied choices and initially selects the first choice.
   *
   * @param choices choices available for selection
   * @throws NullPointerException     if {@code choices} is null
   * @throws IllegalArgumentException if {@code choices} is empty
   */
  public ChooseField( List<?> choices )
  {
    // convert list to array for choices
    this( Objects.requireNonNull( choices, "choices" ).toArray() );
  }

  /***************************************** setChoices ******************************************/
  /**
   * Replaces the available choices with a copy of the supplied array.
   * <p>
   * The current selection is retained when a new choice has the same display text; otherwise the
   * first new choice is selected.
   *
   * @param choices replacement choices
   * @throws NullPointerException     if {@code choices} is null
   * @throws IllegalArgumentException if {@code choices} is empty
   */
  public void setChoices( Object[] choices )
  {
    // a choose field always has a selected item
    Objects.requireNonNull( choices, "choices" );
    if ( choices.length == 0 )
      throw new IllegalArgumentException( "Choices must not be empty" );

    // get current choice before updating choices
    String currentChoice = m_text == null ? "" : getText( getSelectedIndex() );

    // copy choices so later caller changes cannot desynchronise the displayed text
    m_choices = choices.clone();
    m_text = new ArrayList<>( choices.length );
    for ( Object choice : m_choices )
      m_text.add( choice == null ? "null" : choice.toString() );

    // try to find current-choice
    int index = m_text.indexOf( currentChoice );
    setSelectedIndex( index < 0 ? 0 : index );
  }

  /***************************************** setChoices ******************************************/
  /**
   * Replaces the available choices with the contents of the supplied list.
   * <p>
   * The current selection is retained when a new choice has the same display text; otherwise the
   * first new choice is selected.
   *
   * @param choices replacement choices
   * @throws NullPointerException     if {@code choices} is null
   * @throws IllegalArgumentException if {@code choices} is empty
   */
  public void setChoices( List<?> choices )
  {
    // convert list to array for updated choices
    setChoices( Objects.requireNonNull( choices, "choices" ).toArray() );
  }

  /***************************************** changeValue *****************************************/
  /**
   * Steps cyclically through the choices.
   * <p>
   * A positive integral delta moves towards lower indexes and a negative integral delta moves
   * towards higher indexes. Any fractional part is discarded.
   *
   * @param delta number of positions to step, with the direction inverted relative to the index
   */
  @Override
  public void changeValue( double delta )
  {
    // change selected index
    setSelectedIndex( m_index - (int) delta );
  }

  /****************************************** getCount *******************************************/
  /**
   * Returns the number of available choices.
   *
   * @return number of choices, always greater than zero
   */
  public int getCount()
  {
    // return number of items user can choose from
    return m_choices.length;
  }

  /************************************** getSelectedIndex ***************************************/
  /**
   * Returns the index of the selected choice.
   *
   * @return selected index
   */
  public int getSelectedIndex()
  {
    // return currently selected index
    return m_index;
  }

  /************************************** setSelectedIndex ***************************************/
  /**
   * Selects the choice at the supplied index, wrapping indexes outside the available range.
   *
   * @param index index to select; negative and oversized indexes wrap cyclically
   */
  public void setSelectedIndex( int index )
  {
    // set selected index and update displayed text to match
    index = index % m_choices.length;
    m_index = index < 0 ? index + m_choices.length : index;

    setText( getText( m_index ) );
    m_dropdown.scrollToIndex( m_index );
  }

  /******************************************* getText *******************************************/
  /**
   * Returns the display text for a choice.
   *
   * @param index choice index
   * @return display text for the indexed choice
   * @throws IndexOutOfBoundsException if the index is outside the choice range
   */
  public String getText( int index )
  {
    // return text equivalent of object at specified index
    return m_text.get( index );
  }

  /***************************************** getSelected *****************************************/
  /**
   * Returns the currently selected choice object.
   *
   * @return selected choice, which may be null
   */
  public Object getSelected()
  {
    // return currently selected object
    return m_choices[m_index];
  }

  /***************************************** setSelected *****************************************/
  /**
   * Selects a choice equal to the supplied value.
   * <p>
   * If no equal choice exists and the value is a string, the next choice whose display text starts
   * with that string is selected case-insensitively. A null value selects a null choice when one is
   * present, or the first choice otherwise.
   *
   * @param value value or display-text prefix to select
   * @throws InvalidParameterException if no matching choice can be found
   */
  public void setSelected( Object value )
  {
    // set currently selected object
    for ( int index = 0; index < m_choices.length; index++ )
      if ( Objects.equals( m_choices[index], value ) )
      {
        setSelectedIndex( index );
        return;
      }

    // if string try to find choice that starts with this string
    if ( value instanceof String text )
    {
      int index = findNextIndex( text );
      if ( index >= 0 )
      {
        setSelectedIndex( index );
        return;
      }
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
  /**
   * Handles navigation keys and F2 drop-down toggling.
   *
   * @param event key-press event
   */
  @Override
  protected void keyPressed( KeyEvent event )
  {
    // action key press to change current selected item
    switch ( event.getCode() )
    {
      case DOWN:
      case KP_DOWN:
      case RIGHT:
      case KP_RIGHT:
      case PAGE_DOWN:
        changeValue( -1 );
        event.consume();
        break;
      case UP:
      case KP_UP:
      case LEFT:
      case KP_LEFT:
      case PAGE_UP:
        changeValue( 1 );
        event.consume();
        break;
      case HOME:
        setSelectedIndex( 0 );
        event.consume();
        break;
      case END:
        setSelectedIndex( getCount() - 1 );
        event.consume();
        break;
      case F2:
        m_dropdown.toggle();
        event.consume();
        break;
      default:
        break;
    }
  }

  /****************************************** findNext *******************************************/
  /**
   * Selects the next choice whose display text starts with the supplied prefix, ignoring case.
   * <p>
   * Searching begins after the current selection and wraps around. The selection is unchanged when
   * the prefix is empty or no choice matches.
   *
   * @param start display-text prefix to find
   * @throws NullPointerException if {@code start} is null
   */
  public void findNext( String start )
  {
    // find next item that starts with specified string (case-insensitive)
    int index = findNextIndex( start );
    if ( index >= 0 )
      setSelectedIndex( index );
  }

  /*************************************** findNextIndex ****************************************/
  /**
   * Finds the next matching choice index without changing the selection.
   *
   * @param start display-text prefix to find
   * @return matching index, or -1 when the prefix is empty or no choice matches
   * @throws NullPointerException if {@code start} is null
   */
  private int findNextIndex( String start )
  {
    // return next matching item without changing the current selection
    Objects.requireNonNull( start, "start" );
    if ( start.isEmpty() )
      return -1;

    for ( int delta = 1; delta < m_choices.length; delta++ )
    {
      int index = ( m_index + delta ) % m_choices.length;
      String text = m_text.get( index );
      if ( text.regionMatches( true, 0, start, 0, start.length() ) )
        return index;
    }

    return -1;
  }

}
