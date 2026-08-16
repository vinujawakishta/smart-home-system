from docx import Document
from docx.shared import Inches, Pt, RGBColor
from docx.enum.text import WD_ALIGN_PARAGRAPH
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_CELL_VERTICAL_ALIGNMENT
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.enum.section import WD_SECTION

OUT = 'Smart_Home_System_Technical_Report.docx'
doc = Document()
sec = doc.sections[0]
sec.top_margin = Inches(0.72); sec.bottom_margin = Inches(0.7)
sec.left_margin = Inches(0.78); sec.right_margin = Inches(0.78)
sec.header_distance = Inches(0.35); sec.footer_distance = Inches(0.35)

BLUE = RGBColor(31, 78, 120); DARK = RGBColor(11, 37, 69); MUTED = RGBColor(89, 102, 115)
LIGHT = 'E8EEF5'; PALE = 'F4F6F9'; BORDER = 'C8D2DC'

styles = doc.styles
normal = styles['Normal']; normal.font.name = 'Calibri'; normal._element.rPr.rFonts.set(qn('w:ascii'), 'Calibri'); normal._element.rPr.rFonts.set(qn('w:hAnsi'), 'Calibri'); normal.font.size = Pt(10.5)
normal.paragraph_format.space_after = Pt(5); normal.paragraph_format.line_spacing = 1.08
for name, size, color, before, after in [('Heading 1', 16, BLUE, 12, 6), ('Heading 2', 12.5, BLUE, 9, 4), ('Heading 3', 11, DARK, 7, 3)]:
    s = styles[name]; s.font.name = 'Calibri'; s._element.rPr.rFonts.set(qn('w:ascii'), 'Calibri'); s._element.rPr.rFonts.set(qn('w:hAnsi'), 'Calibri'); s.font.size = Pt(size); s.font.bold = True; s.font.color.rgb = color; s.paragraph_format.space_before = Pt(before); s.paragraph_format.space_after = Pt(after)

def set_cell_shading(cell, fill):
    tcPr = cell._tc.get_or_add_tcPr(); shd = OxmlElement('w:shd'); shd.set(qn('w:fill'), fill); tcPr.append(shd)

def set_cell_border(cell, color=BORDER):
    tcPr = cell._tc.get_or_add_tcPr(); borders = tcPr.first_child_found_in('w:tcBorders')
    if borders is None:
        borders = OxmlElement('w:tcBorders'); tcPr.append(borders)
    for edge in ('top','left','bottom','right'):
        e = OxmlElement('w:'+edge); e.set(qn('w:val'),'single'); e.set(qn('w:sz'),'6'); e.set(qn('w:color'),color); borders.append(e)

def cell_text(cell, text, bold=False, size=9.3, color=None, align=None):
    cell.text = ''
    p = cell.paragraphs[0]; p.paragraph_format.space_after = Pt(0); p.paragraph_format.space_before = Pt(0)
    if align is not None: p.alignment = align
    r = p.add_run(text); r.bold = bold; r.font.size = Pt(size); r.font.name = 'Calibri'; r._element.rPr.rFonts.set(qn('w:ascii'),'Calibri'); r._element.rPr.rFonts.set(qn('w:hAnsi'),'Calibri')
    if color: r.font.color.rgb = color
    cell.vertical_alignment = WD_CELL_VERTICAL_ALIGNMENT.CENTER; set_cell_border(cell)

def table(headers, rows, widths=None, font=9.1):
    t = doc.add_table(rows=1, cols=len(headers)); t.alignment = WD_TABLE_ALIGNMENT.CENTER; t.autofit = False
    for i,h in enumerate(headers):
        c=t.rows[0].cells[i]; set_cell_shading(c, LIGHT); cell_text(c,h,True,font,DARK,WD_ALIGN_PARAGRAPH.CENTER)
        if widths: c.width=Inches(widths[i])
    for row in rows:
        cells=t.add_row().cells
        for i,v in enumerate(row):
            cell_text(cells[i],v,False,font)
            if widths: cells[i].width=Inches(widths[i])
    doc.add_paragraph().paragraph_format.space_after = Pt(1)
    return t

def para(text='', bold_lead=None):
    p=doc.add_paragraph()
    if bold_lead and text.startswith(bold_lead):
        p.add_run(bold_lead).bold=True; p.add_run(text[len(bold_lead):])
    else: p.add_run(text)
    return p

def note(label, text):
    t=doc.add_table(rows=1, cols=1); t.autofit=False; c=t.cell(0,0); set_cell_shading(c, PALE); cell_text(c,'',False,9.6)
    p=c.paragraphs[0]; p.clear(); r=p.add_run(label+'  '); r.bold=True; r.font.color.rgb=DARK; p.add_run(text); p.paragraph_format.space_after=Pt(3); p.paragraph_format.space_before=Pt(3)
    doc.add_paragraph().paragraph_format.space_after=Pt(1)

def page_title(title, subtitle=None):
    p=doc.add_paragraph(); p.paragraph_format.space_before=Pt(0); p.paragraph_format.space_after=Pt(3)
    r=p.add_run(title); r.bold=True; r.font.name='Calibri'; r._element.rPr.rFonts.set(qn('w:ascii'),'Calibri'); r.font.size=Pt(24); r.font.color.rgb=DARK
    if subtitle:
        p=doc.add_paragraph(); p.paragraph_format.space_after=Pt(12); r=p.add_run(subtitle); r.italic=True; r.font.size=Pt(11); r.font.color.rgb=MUTED

def footer(section):
    p=section.footer.paragraphs[0]; p.alignment=WD_ALIGN_PARAGRAPH.CENTER; r=p.add_run('Smart Home Monitoring & Control System | Technical Report'); r.font.size=Pt(8); r.font.color.rgb=MUTED
footer(sec)

# PAGE 1
page_title('Smart Home Monitoring & Control System', 'Concise technical report: architecture, synchronization, floor representation and simulator operations')
note('Purpose', 'This report explains how the delivered Android client and web-based hardware simulator share device state, how two floors are represented, and why the selected design is suitable for a small smart-home prototype.')
doc.add_heading('1. System Overview', 1)
para('The system is a cloud-connected smart-home monitoring and control prototype. It contains an Android mobile application for monitoring, navigation, alerts and device control, together with a browser-based simulator that represents the household hardware. Both clients connect to the same Firebase Realtime Database, so a state change made from either interface becomes visible to the other without a manual refresh.')
para('The prototype manages heterogeneous devices: lights, outlets, cameras, multi-switch panels and a safety-critical iron outlet. Each device belongs to a floor and room, and exposes a common operational state: ON, OFF, ERROR or DISCONNECTED. Multi-switch devices additionally retain the state of individual channels.')
doc.add_heading('Scope and Key Design Choices', 2)
table(['Area','Implemented choice','Reason for the choice'],[
['Shared state','Firebase Realtime Database','Provides a hosted, low-latency publish/subscribe data store without operating a custom server.'],
['Mobile client','Kotlin + Jetpack Compose','Compose supports reactive UI updates and compact screen-based navigation.'],
['Hardware simulator','HTML/CSS/JavaScript web dashboard','Runs in any browser and behaves like an accessible virtual hardware panel.'],
['Spatial model','Named rooms plus an abstract 6 x 4 grid','Easy to read, deterministic and appropriate when detailed building drawings are unnecessary.'],
['Safety model','Timed iron cutoff, alerts and event history','Makes safety-related device behaviour observable and auditable.'],
], [1.25,2.0,3.85])
doc.add_heading('Report Structure', 2)
para('The following pages describe the system architecture, data and synchronization mechanism, floor-grid representation, simulator operations, and the rationale and limitations of the approach. The report reflects the current source implementation; it distinguishes completed features from future server-side safety automation.')

# PAGE 2
doc.add_page_break(); page_title('Architecture and Data Model')
doc.add_heading('2. Architecture',1)
para('The architecture is a two-client, cloud-mediated design. The Android application and the browser simulator are independent clients: neither calls the other directly. Firebase Realtime Database is the shared source of truth. This loose coupling lets either client be replaced or extended without changing the other client’s user interface.')
table(['Layer','Component','Responsibilities'],[
['Presentation','Android app (Jetpack Compose)','Dashboard, floor-plan view, device details, reports and alert list.'],
['Presentation','Web simulator (HTML/CSS/JS)','Floor and room filtering; device cards; toggle, error, disconnect, reset and channel controls.'],
['Synchronization','Firebase Realtime Database','Stores shared nodes and pushes changes to subscribed clients.'],
['Safety / audit','Device events and alerts','Records state transitions and presents timed-cutoff notifications.'],
], [1.1,2.1,3.9])
doc.add_heading('Architecture Flow',2)
table(['Android application','Firebase Realtime Database','Web hardware simulator'],[
['User action updates a device field.\n\nValue listeners rebuild Compose state and recompose screens.','/devices\n/alerts\n/deviceEvents\n\nWrites are persisted and distributed to every subscribed client.','Device-card action writes a complete device node.\n\nonValue listener updates local state and re-renders the selected view.'],
], [2.35,2.3,2.35], 9.0)
para('Figure 1. Logical data flow. Arrows are bidirectional between each client and Firebase; all synchronization passes through the central database.', bold_lead='Figure 1. ')
doc.add_heading('Shared Device Contract',2)
para('A device record contains an identifier, type, name, floorId, roomId, state, descriptive details and, where required, a Boolean channel list. The Android model adds maxOnDuration and turnedOnAt for timed appliances. Both clients use the same floor and room identifiers; therefore the display hierarchy is independent of the database key order.')
table(['Node','Representative fields','Purpose'],[
['/devices/{deviceId}','type, name, floorId, roomId, state, channels, details','Live operational state and placement.'],
['/alerts/{alertId}','deviceId, deviceName, message, timestamp','Safety or fault messages for the Alerts screen.'],
['/deviceEvents/{eventId}','deviceId, deviceName, toState, timestamp','Chronological record used by the Reports screen.'],
], [1.55,3.1,2.35])

# PAGE 3
doc.add_page_break(); page_title('Synchronization Mechanism')
doc.add_heading('3. Event-Driven Bidirectional Synchronization',1)
para('Synchronization is implemented with Firebase Realtime Database listeners and writes. At startup, the web simulator attaches an onValue listener to /devices. The Android app attaches ValueEventListeners to /devices, /alerts and /deviceEvents. Each listener first supplies the current snapshot and then fires again whenever the relevant database node changes.')
para('When the user operates a control, the client writes the changed data to Firebase. The client does not treat its own temporary local value as authoritative. Instead, the incoming database event updates the local collection and triggers a full UI render or Jetpack Compose recomposition. This produces the same visible state after a web action, a mobile action or an external database update.')
doc.add_heading('Write and Read Sequence',2)
table(['Step','Action','Outcome'],[
['1','A user toggles a device or a switch channel.','The client computes the new state from the latest in-memory device record.'],
['2','The client writes to the relevant /devices child.','Firebase persists the canonical value. The web client uses full-node set() for a complete device update; Android uses field-level setValue() calls.'],
['3','Firebase notifies subscribed listeners.','Every open client receives the changed snapshot, including the originating client.'],
['4','Each UI updates from the snapshot.','Cards, floor markers, counters, reports and alerts show the same current state.'],
], [0.55,2.85,3.6])
doc.add_heading('Why This Mechanism Was Chosen',2)
para('Realtime Database is a strong fit because the prototype needs small, frequently changing records rather than complex relational queries. Its listener model maps naturally to device telemetry and controls: a database update becomes a UI update. Firebase also removes the need to build, host and secure a separate REST or WebSocket server for this mini-project, reducing implementation time while keeping the architecture realistic for an IoT demonstration.')
note('Consistency note', 'The current design is last-write-wins at the affected database location. This is acceptable for a demonstration with occasional human commands. A production system should use transactions or server-side validation for competing writes, especially for safety-critical devices.')
doc.add_heading('Seeding and Recovery',2)
para('The web simulator includes a one-time initialization listener. If /devices is empty, it writes the predefined device seed only once. A Reset action restores the selected device to its seed configuration. This makes demonstrations repeatable while avoiding accidental reseeding of an existing database.')

# PAGE 4
doc.add_page_break(); page_title('Floor Representation')
doc.add_heading('4. Multi-Floor Spatial Model',1)
para('The system represents two floors with named rooms and a simple abstract grid. The floor/room hierarchy is static application metadata, while device state is stored in Firebase. Separating the two keeps the database compact and prevents a device state update from changing the structural layout.')
table(['Floor','Rooms'],[
['1st Floor','Entrance, Living Room, Kitchen, Bathroom, Staircase'],
['2nd Floor','Master Bedroom, Bedroom 2, Study Room, Bathroom, Balcony'],
], [1.3,5.7])
doc.add_heading('Abstract 6 x 4 Grid',2)
para('In the Android Floor Plan screen, both floors use GRID_COLS = 6 and GRID_ROWS = 4. Each room is defined by a room identifier, starting column and row, and column/row span. The UI draws the resulting room rectangles on a Canvas and places small device markers inside their assigned rooms. Selecting a marker opens the corresponding device detail screen.')
table(['Floor 1 arrangement','Floor 2 arrangement'],[
['Columns 1-2: Entrance (row 1), Staircase (row 2), Bathroom (rows 3-4).\n\nColumns 3-4: Living Room (rows 1-4).\n\nColumns 5-6: Kitchen (rows 1-4).','Columns 1-2: Balcony (rows 1-2), Bathroom (rows 3-4).\n\nColumns 3-4: Master Bedroom (rows 1-4).\n\nColumns 5-6: Bedroom 2 (rows 1-2), Study Room (rows 3-4).'],
], [3.5,3.5], 9.2)
doc.add_heading('Why an Abstract Grid Was Chosen',2)
para('The requirement calls for an abstract grid over floor layouts, not a surveyed architectural plan. A fixed grid gives a clear visual map on many mobile screen sizes, makes room positions repeatable in code, and avoids copyright, scaling and device-placement issues associated with imported plan images. It is also simple to extend: a future floor needs only a list of RoomLayout rectangles and room metadata.')
para('The web simulator uses the same logical hierarchy, although it presents it as floor and room filters rather than a drawn Canvas. This gives users a fast operational console while keeping device placement consistent with the mobile floor plan.')
note('Interpretation', 'The grid is a navigational representation of rooms, not an exact construction drawing. Device dots communicate room membership and status; they do not claim a precise physical installation coordinate.')

# PAGE 5
doc.add_page_break(); page_title('Simulator Operations and Device Behaviour')
doc.add_heading('5. Web Hardware Simulator',1)
para('The web simulator is a companion hardware dashboard. It lets a demonstrator act as a smart-home device or hardware gateway while the Android app acts as the resident’s controller. The dashboard filters devices by selected floor and room, groups cards by room and shows totals for visible devices, powered-on devices and alert states.')
table(['Operation','Simulator behaviour','Shared result'],[
['Toggle','Changes ON to OFF or OFF to ON.','Updated state is written to /devices and appears in the mobile app.'],
['Error','Sets the device state to ERROR.','Both clients render an error indicator and count it as an alert state.'],
['Disconnect','Sets state to DISCONNECTED.','Represents a network or hardware communication fault.'],
['Reset','Restores the device’s factory seed record.','Returns demo data to a known state.'],
['Channel toggle','Changes one multi-switch channel; parent is ON when any channel is ON.','Supports 2-, 3-, 4- and 5-gang panels as one device entity.'],
], [1.25,3.25,2.5])
doc.add_heading('Mobile Application Operations',2)
para('The Android app provides a Home/dashboard view, Floor Plan view, Reports view, Alerts view and device-detail navigation. Device details provide type-specific controls. Cameras show a mock feed state, multi-switches provide per-channel controls, and timed appliances expose a maximum on-duration configuration.')
doc.add_heading('Safety and Reporting',2)
para('For the iron outlet, the application stores the moment it was switched on and an optional permitted duration. While the app is open, a countdown can invoke a client-side fallback that turns the iron OFF and writes an alert. State changes are also recorded as device events. The Reports screen pairs ON and OFF events to estimate device on-time and displays safety-cutoff information; Alerts provides the detailed alert history.')
doc.add_heading('Why These Operational Mechanisms Were Chosen',2)
para('Specialized device profiles make the simulator more credible than a collection of identical switches. A multi-switch is represented as a single physical panel with individual channels, cameras expose meaningful no-signal/error states, and a timed iron demonstrates that smart-home control must consider safety as well as convenience. Device events and alerts provide enough traceability for a classroom demonstration without introducing unnecessary analytics infrastructure.')

# PAGE 6
doc.add_page_break(); page_title('Justification, Constraints and Conclusion')
doc.add_heading('6. Design Justification',1)
table(['Decision','Justification','Trade-off / future improvement'],[
['Central Firebase state','One accessible source of truth enables cross-device demonstration and simplifies synchronization.','Add security rules, authentication and server validation before deployment.'],
['Reactive listeners','Eliminates polling and manual refresh; screens react to the database snapshot.','Use narrower listeners or pagination if device/event volume grows.'],
['Static floor metadata','Stable layouts are easy to test and do not need to be downloaded with every state change.','Move layouts to a versioned configuration if users must edit plans.'],
['Canvas grid map','Compact and responsive way to show room context on a phone.','Add pan/zoom or true scaled plans only if accurate coordinates are required.'],
['Client cutoff fallback','Demonstrates immediate safety response and alert generation.','A scheduled Cloud Function or worker must enforce cutoffs even when clients are closed.'],
], [1.45,3.15,2.4], 8.8)
doc.add_heading('Known Constraints',2)
para('The architecture currently prioritizes demonstrability. The mobile safety cutoff is explicitly client-side, so it is not a guarantee when no app is running. The source notes that a future deployed backend job or Cloud Function should perform the same cutoff independently. In addition, concurrent writes to the same device can follow last-write-wins behaviour; production controls should apply server-side authorization, validation and transactional updates where necessary.')
doc.add_heading('Conclusion',2)
para('The system uses a clear cloud-mediated architecture: the Android client and web simulator exchange device state through Firebase Realtime Database, while listeners keep both interfaces synchronized. A shared floor/room model and simple 6 x 4 grid provide understandable spatial context without the cost of full architectural mapping. The simulator’s operational controls, specialized device models, alerts and event reporting make the prototype suitable for demonstrating realistic smart-home monitoring, control and safety scenarios.')
doc.add_heading('Demonstration Checklist',2)
para('A concise demonstration can show: (1) select a floor and room; (2) toggle a light in the web simulator and observe the Android update; (3) toggle an Android switch channel and observe the simulator update; (4) set a camera to disconnected or error; (5) configure and trigger the timed iron safety flow; and (6) show the alert and report history.')
para('Source basis: WebSimulator/app.js and SmartHomeSimulator/app/src/main/java/com/example/smarthomesimulator/MainActivity.kt.', bold_lead='Source basis: ')

doc.save(OUT)
print(OUT)
