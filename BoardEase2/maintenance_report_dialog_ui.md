# Maintenance Report Dialog UI Design

## Overview
A beautiful, modern UI design for the "Report for Maintenance" button popup modal in the Current Boarding House Booked section. This is the UI-only implementation - functionality will be added later.

## Location
- **Layout File**: `app/src/main/res/layout/dialog_report_maintenance.xml`
- **Java Method**: `BoarderBookingFragment.showMaintenanceReportDialog()`
- **Trigger**: Clicking "Report for Maintenance" button in the booking details dialog

## UI Features

### 1. Header Section
- **Maintenance Icon**: Orange maintenance icon (#FF6B35) on the left
- **Title**: "Report for Maintenance" in bold Poppins font
- **Close Button**: X button in the top right corner

### 2. Info Text
- Helpful description text explaining the purpose of the form
- Styled in gray (#666666) with proper line spacing

### 3. Form Fields

#### Title Field
- Single-line text input
- Icon: Edit icon (orange)
- Placeholder: "Title"
- Input type: Text with sentence capitalization

#### Maintenance Type & Priority (Side by Side)
- **Maintenance Type**: Dropdown spinner
  - Icon: Tools icon (orange)
  - Options: (To be populated programmatically)
    - Plumbing
    - Electrical
    - HVAC
    - Appliances
    - Furniture
    - Other
  
- **Priority**: Dropdown spinner
  - Icon: Info icon (orange)
  - Options: (To be populated programmatically)
    - Low
    - Medium
    - High
    - Urgent

#### Location Field
- Text input with multi-line support
- Icon: Location icon (orange)
- Placeholder: "e.g., Room 101, Bathroom, Common Area"
- Input type: Text with sentence capitalization

#### Description Field
- Multi-line text input (4-6 lines)
- Icon: Edit icon (orange)
- Placeholder: "Please describe the maintenance issue in detail..."
- Input type: Multi-line text with sentence capitalization

#### Contact Phone Field
- Single-line text input
- Icon: Phone icon (orange)
- Placeholder: "e.g., +63 912 345 6789"
- Input type: Phone number

#### Preferred Date & Time (Side by Side)
- **Preferred Date**: Outlined button
  - Icon: Calendar icon (orange)
  - Text: "Select Date"
  - Opens date picker (functionality to be added)
  
- **Preferred Time**: Outlined button
  - Icon: Clock icon (orange)
  - Text: "Select Time"
  - Opens time picker (functionality to be added)

### 4. Image Attachment Section
- **Card Container**: Light gray background (#F9FAFB)
- **Section Title**: "Attach Photos (Optional)"
- **Add Photo Button**: Outlined button with camera icon
- **Image Grid**: RecyclerView for displaying selected images (hidden by default)
- **Helper Text**: "You can attach up to 5 photos to help us understand the issue better"
- Maximum 5 photos allowed (validation to be added)

### 5. Action Buttons
- **Cancel Button**: Outlined button with gray text
  - Dismisses the dialog
  
- **Submit Report Button**: Filled button with orange background (#FF6B35)
  - Icon: Maintenance icon
  - Text: "Submit Report"
  - Currently shows a toast message (functionality to be added)

## Design Specifications

### Colors
- **Primary Orange**: #FF6B35 (maintenance theme color)
- **Text Primary**: #000000 (black)
- **Text Secondary**: #666666 (gray)
- **Text Tertiary**: #999999 (light gray)
- **Background**: #FFFFFF (white)
- **Card Background**: #F9FAFB (light gray)
- **Border Color**: #E0E0E0 (light gray)
- **Button Background**: #FF6B35 (orange)

### Typography
- **Font Family**: Poppins
- **Header Title**: 20sp, Bold
- **Section Titles**: 14sp, Medium
- **Input Text**: 14sp, Regular
- **Helper Text**: 11-12sp, Regular
- **Button Text**: 13-14sp, Medium

### Spacing
- **Dialog Padding**: 24dp
- **Field Margin Bottom**: 16dp
- **Section Margin**: 20dp
- **Button Padding**: 12-16dp vertical, 24dp horizontal

### Border Radius
- **Text Input Fields**: 8dp
- **Cards**: 12dp
- **Buttons**: 20dp (rounded) or 8dp (outlined)

### Icons Used
- `ic_maintenance.png` - Maintenance icon
- `ic_close.xml` - Close button
- `ic_edit.png` - Edit/Text icon
- `ic_tools.xml` - Tools/Maintenance type icon
- `ic_info.xml` - Info/Priority icon
- `location.png` - Location icon
- `ic_phone.xml` - Phone icon
- `ic_calendar.xml` - Calendar icon
- `ic_clock.xml` - Clock/Time icon
- `ic_camera1.xml` - Camera icon

## Current Implementation Status

### ✅ Completed (UI Only)
- Dialog layout created
- All form fields designed
- Button layouts and styling
- Image attachment section UI
- Dialog display functionality
- Close and cancel button functionality

### ⏳ To Be Implemented (Functionality)
- Form validation
- Maintenance type spinner population
- Priority spinner population
- Date picker functionality
- Time picker functionality
- Image selection and display
- Image upload functionality
- Form submission to backend
- API integration
- Success/error handling
- Loading states

## Usage

The dialog is triggered when a user:
1. Opens the "Current Boarding House Booked" section
2. Clicks on a booking card
3. Clicks the "Report for Maintenance" button in the booking details dialog

The maintenance report dialog will appear with all the form fields ready for input (UI only - submission functionality pending).

## Next Steps

1. **Populate Spinners**: Add arrays for maintenance types and priorities
2. **Date/Time Pickers**: Implement date and time picker dialogs
3. **Image Picker**: Implement image selection from gallery/camera
4. **Form Validation**: Add validation for required fields
5. **API Integration**: Connect to backend maintenance request endpoint
6. **Error Handling**: Add proper error messages and loading states
7. **Success Feedback**: Show success message and close dialog on successful submission

## Notes

- The dialog is scrollable with a max height of 600dp
- All icons are tinted with the maintenance orange color (#FF6B35)
- The design follows Material Design principles
- The UI is responsive and works on different screen sizes
- The dialog background uses the existing `dialog_background` drawable for consistency

