// Auto-generated DO NOT EDIT

#include <X11/Xlib.h>
#include <X11/Xutil.h>
#include <X11/XKBlib.h>
#include <X11/Xresource.h>
#include <stddef.h>
#include <stdio.h>

int _Xmblen(char * _a0, int _a1) { return 0; }
XFontStruct * XQueryFont(Display * _a0, XID _a1) { return 0; }
XTimeCoord * XGetMotionEvents(Display * _a0, Window _a1, Time _a2, Time _a3, int * _a4) { return 0; }
XModifierKeymap * XDeleteModifiermapEntry(XModifierKeymap * _a0, KeyCode _a1, int _a2) { return 0; }
XModifierKeymap * XInsertModifiermapEntry(XModifierKeymap * _a0, KeyCode _a1, int _a2) { return 0; }
XModifierKeymap * XNewModifiermap(int _a0) { return 0; }
int XInitImage(XImage * _a0) { return 0; }
XImage * XGetImage(Display * _a0, Drawable _a1, int _a2, int _a3, unsigned int _a4, unsigned int _a5, unsigned long _a6, int _a7) { return 0; }
XImage * XGetSubImage(Display * _a0, Drawable _a1, int _a2, int _a3, unsigned int _a4, unsigned int _a5, unsigned long _a6, int _a7, XImage * _a8, int _a9, int _a10) { return 0; }
void XrmInitialize(void) { (void)0; }
char * XFetchBytes(Display * _a0, int * _a1) { return 0; }
char * XFetchBuffer(Display * _a0, int * _a1, int _a2) { return 0; }
int XGetAtomNames(Display * _a0, Atom * _a1, int _a2, char ** _a3) { return 0; }
int XInternAtoms(Display * _a0, char ** _a1, int _a2, int _a3, Atom * _a4) { return 0; }
Colormap XCopyColormapAndFree(Display * _a0, Colormap _a1) { return 0; }
Cursor XCreateGlyphCursor(Display * _a0, Font _a1, Font _a2, unsigned int _a3, unsigned int _a4, const XColor * _a5, const XColor * _a6) { return 0; }
Font XLoadFont(Display * _a0, const char * _a1) { return 0; }
GContext XGContextFromGC(GC _a0) { return 0; }
void XFlushGC(Display * _a0, GC _a1) { (void)0; }
Pixmap XCreatePixmapFromBitmapData(Display * _a0, Drawable _a1, char * _a2, unsigned int _a3, unsigned int _a4, unsigned long _a5, unsigned long _a6, unsigned int _a7) { return 0; }
Window XCreateSimpleWindow(Display * _a0, Window _a1, int _a2, int _a3, unsigned int _a4, unsigned int _a5, unsigned int _a6, unsigned long _a7, unsigned long _a8) { return 0; }
Colormap * XListInstalledColormaps(Display * _a0, Window _a1, int * _a2) { return 0; }
char ** XListFonts(Display * _a0, const char * _a1, int _a2, int * _a3) { return 0; }
char ** XListFontsWithInfo(Display * _a0, const char * _a1, int _a2, int * _a3, XFontStruct ** _a4) { return 0; }
char ** XGetFontPath(Display * _a0, int * _a1) { return 0; }
Atom * XListProperties(Display * _a0, Window _a1, int * _a2) { return 0; }
XHostAddress * XListHosts(Display * _a0, int * _a1, int * _a2) { return 0; }
KeySym * XGetKeyboardMapping(Display * _a0, KeyCode _a1, int _a2, int * _a3) { return 0; }
KeySym XStringToKeysym(const char * _a0) { return 0; }
long XMaxRequestSize(Display * _a0) { return 0; }
long XExtendedMaxRequestSize(Display * _a0) { return 0; }
char * XResourceManagerString(Display * _a0) { return 0; }
char * XScreenResourceString(Screen * _a0) { return 0; }
unsigned long XDisplayMotionBufferSize(Display * _a0) { return 0; }
void XLockDisplay(Display * _a0) { (void)0; }
void XUnlockDisplay(Display * _a0) { (void)0; }
XExtData ** XEHeadOfExtensionList(XEDataObject _a0) { return 0; }
Window XRootWindowOfScreen(Screen * _a0) { return 0; }
Visual * XDefaultVisualOfScreen(Screen * _a0) { return 0; }
GC XDefaultGC(Display * _a0, int _a1) { return 0; }
GC XDefaultGCOfScreen(Screen * _a0) { return 0; }
unsigned long XAllPlanes(void) { return 0; }
unsigned long XBlackPixelOfScreen(Screen * _a0) { return 0; }
unsigned long XWhitePixelOfScreen(Screen * _a0) { return 0; }
unsigned long XLastKnownRequestProcessed(Display * _a0) { return 0; }
char * XServerVendor(Display * _a0) { return 0; }
Colormap XDefaultColormapOfScreen(Screen * _a0) { return 0; }
Display * XDisplayOfScreen(Screen * _a0) { return 0; }
long XEventMaskOfScreen(Screen * _a0) { return 0; }
void XSetIOErrorExitHandler(Display * _a0, XIOErrorExitHandler _a1, void * _a2) { (void)0; }
int * XListDepths(Display * _a0, int _a1, int * _a2) { return 0; }
int XReconfigureWMWindow(Display * _a0, Window _a1, int _a2, unsigned int _a3, XWindowChanges * _a4) { return 0; }
int XGetWMProtocols(Display * _a0, Window _a1, Atom ** _a2, int * _a3) { return 0; }
int XGetCommand(Display * _a0, Window _a1, char *** _a2, int * _a3) { return 0; }
int XGetWMColormapWindows(Display * _a0, Window _a1, Window ** _a2, int * _a3) { return 0; }
int XSetWMColormapWindows(Display * _a0, Window _a1, Window * _a2, int _a3) { return 0; }
int XActivateScreenSaver(Display * _a0) { return 0; }
int XAddHost(Display * _a0, XHostAddress * _a1) { return 0; }
int XAddHosts(Display * _a0, XHostAddress * _a1, int _a2) { return 0; }
int XAddToExtensionList(struct _XExtData ** _a0, XExtData * _a1) { return 0; }
int XAddToSaveSet(Display * _a0, Window _a1) { return 0; }
int XAllocColor(Display * _a0, Colormap _a1, XColor * _a2) { return 0; }
int XAllocColorCells(Display * _a0, Colormap _a1, int _a2, unsigned long * _a3, unsigned int _a4, unsigned long * _a5, unsigned int _a6) { return 0; }
int XAllocColorPlanes(Display * _a0, Colormap _a1, int _a2, unsigned long * _a3, int _a4, int _a5, int _a6, int _a7, unsigned long * _a8, unsigned long * _a9, unsigned long * _a10) { return 0; }
int XAllocNamedColor(Display * _a0, Colormap _a1, const char * _a2, XColor * _a3, XColor * _a4) { return 0; }
int XAllowEvents(Display * _a0, int _a1, Time _a2) { return 0; }
int XBell(Display * _a0, int _a1) { return 0; }
int XBitmapBitOrder(Display * _a0) { return 0; }
int XBitmapPad(Display * _a0) { return 0; }
int XBitmapUnit(Display * _a0) { return 0; }
int XCellsOfScreen(Screen * _a0) { return 0; }
int XChangeActivePointerGrab(Display * _a0, unsigned int _a1, Cursor _a2, Time _a3) { return 0; }
int XChangeGC(Display * _a0, GC _a1, unsigned long _a2, XGCValues * _a3) { return 0; }
int XChangeKeyboardControl(Display * _a0, unsigned long _a1, XKeyboardControl * _a2) { return 0; }
int XChangeKeyboardMapping(Display * _a0, int _a1, int _a2, KeySym * _a3, int _a4) { return 0; }
int XChangeSaveSet(Display * _a0, Window _a1, int _a2) { return 0; }
int XCirculateSubwindows(Display * _a0, Window _a1, int _a2) { return 0; }
int XCirculateSubwindowsDown(Display * _a0, Window _a1) { return 0; }
int XCirculateSubwindowsUp(Display * _a0, Window _a1) { return 0; }
int XClearArea(Display * _a0, Window _a1, int _a2, int _a3, unsigned int _a4, unsigned int _a5, int _a6) { return 0; }
int XConfigureWindow(Display * _a0, Window _a1, unsigned int _a2, XWindowChanges * _a3) { return 0; }
int XCopyArea(Display * _a0, Drawable _a1, Drawable _a2, GC _a3, int _a4, int _a5, unsigned int _a6, unsigned int _a7, int _a8, int _a9) { return 0; }
int XCopyGC(Display * _a0, GC _a1, unsigned long _a2, GC _a3) { return 0; }
int XCopyPlane(Display * _a0, Drawable _a1, Drawable _a2, GC _a3, int _a4, int _a5, unsigned int _a6, unsigned int _a7, int _a8, int _a9, unsigned long _a10) { return 0; }
int XDefaultDepthOfScreen(Screen * _a0) { return 0; }
int XDestroySubwindows(Display * _a0, Window _a1) { return 0; }
int XDoesBackingStore(Screen * _a0) { return 0; }
int XDoesSaveUnders(Screen * _a0) { return 0; }
int XDisableAccessControl(Display * _a0) { return 0; }
int XDisplayCells(Display * _a0, int _a1) { return 0; }
int XDisplayPlanes(Display * _a0, int _a1) { return 0; }
int XDrawArc(Display * _a0, Drawable _a1, GC _a2, int _a3, int _a4, unsigned int _a5, unsigned int _a6, int _a7, int _a8) { return 0; }
int XDrawArcs(Display * _a0, Drawable _a1, GC _a2, XArc * _a3, int _a4) { return 0; }
int XDrawImageString(Display * _a0, Drawable _a1, GC _a2, int _a3, int _a4, const char * _a5, int _a6) { return 0; }
int XDrawImageString16(Display * _a0, Drawable _a1, GC _a2, int _a3, int _a4, const XChar2b * _a5, int _a6) { return 0; }
int XDrawLine(Display * _a0, Drawable _a1, GC _a2, int _a3, int _a4, int _a5, int _a6) { return 0; }
int XDrawLines(Display * _a0, Drawable _a1, GC _a2, XPoint * _a3, int _a4, int _a5) { return 0; }
int XDrawPoint(Display * _a0, Drawable _a1, GC _a2, int _a3, int _a4) { return 0; }
int XDrawPoints(Display * _a0, Drawable _a1, GC _a2, XPoint * _a3, int _a4, int _a5) { return 0; }
int XDrawRectangles(Display * _a0, Drawable _a1, GC _a2, XRectangle * _a3, int _a4) { return 0; }
int XDrawSegments(Display * _a0, Drawable _a1, GC _a2, XSegment * _a3, int _a4) { return 0; }
int XDrawString16(Display * _a0, Drawable _a1, GC _a2, int _a3, int _a4, const XChar2b * _a5, int _a6) { return 0; }
int XDrawText(Display * _a0, Drawable _a1, GC _a2, int _a3, int _a4, XTextItem * _a5, int _a6) { return 0; }
int XDrawText16(Display * _a0, Drawable _a1, GC _a2, int _a3, int _a4, XTextItem16 * _a5, int _a6) { return 0; }
int XEnableAccessControl(Display * _a0) { return 0; }
int XFetchName(Display * _a0, Window _a1, char ** _a2) { return 0; }
int XFillArc(Display * _a0, Drawable _a1, GC _a2, int _a3, int _a4, unsigned int _a5, unsigned int _a6, int _a7, int _a8) { return 0; }
int XFillArcs(Display * _a0, Drawable _a1, GC _a2, XArc * _a3, int _a4) { return 0; }
int XFillPolygon(Display * _a0, Drawable _a1, GC _a2, XPoint * _a3, int _a4, int _a5, int _a6) { return 0; }
int XFillRectangles(Display * _a0, Drawable _a1, GC _a2, XRectangle * _a3, int _a4) { return 0; }
int XForceScreenSaver(Display * _a0, int _a1) { return 0; }
int XFreeColormap(Display * _a0, Colormap _a1) { return 0; }
int XFreeColors(Display * _a0, Colormap _a1, unsigned long * _a2, int _a3, unsigned long _a4) { return 0; }
int XFreeFontInfo(char ** _a0, XFontStruct * _a1, int _a2) { return 0; }
int XFreeFontNames(char ** _a0) { return 0; }
int XFreeFontPath(char ** _a0) { return 0; }
int XGeometry(Display * _a0, int _a1, const char * _a2, const char * _a3, unsigned int _a4, unsigned int _a5, unsigned int _a6, int _a7, int _a8, int * _a9, int * _a10, int * _a11, int * _a12) { return 0; }
int XGetFontProperty(XFontStruct * _a0, Atom _a1, unsigned long * _a2) { return 0; }
int XGetGCValues(Display * _a0, GC _a1, unsigned long _a2, XGCValues * _a3) { return 0; }
int XGetGeometry(Display * _a0, Drawable _a1, Window * _a2, int * _a3, int * _a4, unsigned int * _a5, unsigned int * _a6, unsigned int * _a7, unsigned int * _a8) { return 0; }
int XGetIconName(Display * _a0, Window _a1, char ** _a2) { return 0; }
int XGetPointerMapping(Display * _a0, unsigned char * _a1, int _a2) { return 0; }
int XGetScreenSaver(Display * _a0, int * _a1, int * _a2, int * _a3, int * _a4) { return 0; }
int XGetTransientForHint(Display * _a0, Window _a1, Window * _a2) { return 0; }
int XGrabButton(Display * _a0, unsigned int _a1, unsigned int _a2, Window _a3, int _a4, unsigned int _a5, int _a6, int _a7, Window _a8, Cursor _a9) { return 0; }
int XGrabKey(Display * _a0, int _a1, unsigned int _a2, Window _a3, int _a4, int _a5, int _a6) { return 0; }
int XHeightMMOfScreen(Screen * _a0) { return 0; }
int XImageByteOrder(Display * _a0) { return 0; }
int XKillClient(Display * _a0, XID _a1) { return 0; }
int XLookupColor(Display * _a0, Colormap _a1, const char * _a2, XColor * _a3, XColor * _a4) { return 0; }
int XLowerWindow(Display * _a0, Window _a1) { return 0; }
int XMapSubwindows(Display * _a0, Window _a1) { return 0; }
int XMaxCmapsOfScreen(Screen * _a0) { return 0; }
int XMinCmapsOfScreen(Screen * _a0) { return 0; }
int XNoOp(Display * _a0) { return 0; }
int XParseGeometry(const char * _a0, int * _a1, int * _a2, unsigned int * _a3, unsigned int * _a4) { return 0; }
int XPeekIfEvent(Display * _a0, XEvent * _a1, int (*_a2)(Display *, XEvent *, XPointer), XPointer _a3) { return 0; }
int XPlanesOfScreen(Screen * _a0) { return 0; }
int XProtocolRevision(Display * _a0) { return 0; }
int XProtocolVersion(Display * _a0) { return 0; }
int XPutBackEvent(Display * _a0, XEvent * _a1) { return 0; }
int XQLength(Display * _a0) { return 0; }
int XQueryBestCursor(Display * _a0, Drawable _a1, unsigned int _a2, unsigned int _a3, unsigned int * _a4, unsigned int * _a5) { return 0; }
int XQueryBestSize(Display * _a0, int _a1, Drawable _a2, unsigned int _a3, unsigned int _a4, unsigned int * _a5, unsigned int * _a6) { return 0; }
int XQueryBestStipple(Display * _a0, Drawable _a1, unsigned int _a2, unsigned int _a3, unsigned int * _a4, unsigned int * _a5) { return 0; }
int XQueryBestTile(Display * _a0, Drawable _a1, unsigned int _a2, unsigned int _a3, unsigned int * _a4, unsigned int * _a5) { return 0; }
int XQueryColor(Display * _a0, Colormap _a1, XColor * _a2) { return 0; }
int XQueryColors(Display * _a0, Colormap _a1, XColor * _a2, int _a3) { return 0; }
int XQueryTextExtents(Display * _a0, XID _a1, const char * _a2, int _a3, int * _a4, int * _a5, int * _a6, XCharStruct * _a7) { return 0; }
int XQueryTextExtents16(Display * _a0, XID _a1, const XChar2b * _a2, int _a3, int * _a4, int * _a5, int * _a6, XCharStruct * _a7) { return 0; }
int XReadBitmapFile(Display * _a0, Drawable _a1, const char * _a2, unsigned int * _a3, unsigned int * _a4, Pixmap * _a5, int * _a6, int * _a7) { return 0; }
int XReadBitmapFileData(const char * _a0, unsigned int * _a1, unsigned int * _a2, unsigned char ** _a3, int * _a4, int * _a5) { return 0; }
int XRebindKeysym(Display * _a0, KeySym _a1, KeySym * _a2, int _a3, const unsigned char * _a4, int _a5) { return 0; }
int XRecolorCursor(Display * _a0, Cursor _a1, XColor * _a2, XColor * _a3) { return 0; }
int XRemoveFromSaveSet(Display * _a0, Window _a1) { return 0; }
int XRemoveHost(Display * _a0, XHostAddress * _a1) { return 0; }
int XRemoveHosts(Display * _a0, XHostAddress * _a1, int _a2) { return 0; }
int XRestackWindows(Display * _a0, Window * _a1, int _a2) { return 0; }
int XRotateBuffers(Display * _a0, int _a1) { return 0; }
int XRotateWindowProperties(Display * _a0, Window _a1, Atom * _a2, int _a3, int _a4) { return 0; }
int XSetAccessControl(Display * _a0, int _a1) { return 0; }
int XSetArcMode(Display * _a0, GC _a1, int _a2) { return 0; }
int XSetBackground(Display * _a0, GC _a1, unsigned long _a2) { return 0; }
int XSetClipMask(Display * _a0, GC _a1, Pixmap _a2) { return 0; }
int XSetClipOrigin(Display * _a0, GC _a1, int _a2, int _a3) { return 0; }
int XSetClipRectangles(Display * _a0, GC _a1, int _a2, int _a3, XRectangle * _a4, int _a5, int _a6) { return 0; }
int XSetCloseDownMode(Display * _a0, int _a1) { return 0; }
int XSetCommand(Display * _a0, Window _a1, char ** _a2, int _a3) { return 0; }
int XSetDashes(Display * _a0, GC _a1, int _a2, const char * _a3, int _a4) { return 0; }
int XSetFillRule(Display * _a0, GC _a1, int _a2) { return 0; }
int XSetFillStyle(Display * _a0, GC _a1, int _a2) { return 0; }
int XSetFont(Display * _a0, GC _a1, Font _a2) { return 0; }
int XSetFontPath(Display * _a0, char ** _a1, int _a2) { return 0; }
int XSetFunction(Display * _a0, GC _a1, int _a2) { return 0; }
int XSetGraphicsExposures(Display * _a0, GC _a1, int _a2) { return 0; }
int XSetIconName(Display * _a0, Window _a1, const char * _a2) { return 0; }
int XSetLineAttributes(Display * _a0, GC _a1, unsigned int _a2, int _a3, int _a4, int _a5) { return 0; }
int XSetModifierMapping(Display * _a0, XModifierKeymap * _a1) { return 0; }
int XSetPlaneMask(Display * _a0, GC _a1, unsigned long _a2) { return 0; }
int XSetPointerMapping(Display * _a0, const unsigned char * _a1, int _a2) { return 0; }
int XSetScreenSaver(Display * _a0, int _a1, int _a2, int _a3, int _a4) { return 0; }
int XSetState(Display * _a0, GC _a1, unsigned long _a2, unsigned long _a3, int _a4, unsigned long _a5) { return 0; }
int XSetStipple(Display * _a0, GC _a1, Pixmap _a2) { return 0; }
int XSetSubwindowMode(Display * _a0, GC _a1, int _a2) { return 0; }
int XSetTSOrigin(Display * _a0, GC _a1, int _a2, int _a3) { return 0; }
int XSetTile(Display * _a0, GC _a1, Pixmap _a2) { return 0; }
int XSetWindowBackgroundPixmap(Display * _a0, Window _a1, Pixmap _a2) { return 0; }
int XSetWindowBorder(Display * _a0, Window _a1, unsigned long _a2) { return 0; }
int XSetWindowBorderPixmap(Display * _a0, Window _a1, Pixmap _a2) { return 0; }
int XSetWindowBorderWidth(Display * _a0, Window _a1, unsigned int _a2) { return 0; }
int XSetWindowColormap(Display * _a0, Window _a1, Colormap _a2) { return 0; }
int XStoreBuffer(Display * _a0, const char * _a1, int _a2, int _a3) { return 0; }
int XStoreBytes(Display * _a0, const char * _a1, int _a2) { return 0; }
int XStoreColor(Display * _a0, Colormap _a1, XColor * _a2) { return 0; }
int XStoreNamedColor(Display * _a0, Colormap _a1, const char * _a2, unsigned long _a3, int _a4) { return 0; }
int XTextExtents16(XFontStruct * _a0, const XChar2b * _a1, int _a2, int * _a3, int * _a4, int * _a5, XCharStruct * _a6) { return 0; }
int XTextWidth(XFontStruct * _a0, const char * _a1, int _a2) { return 0; }
int XTextWidth16(XFontStruct * _a0, const XChar2b * _a1, int _a2) { return 0; }
int XUngrabButton(Display * _a0, unsigned int _a1, unsigned int _a2, Window _a3) { return 0; }
int XUngrabKey(Display * _a0, int _a1, unsigned int _a2, Window _a3) { return 0; }
int XUnmapSubwindows(Display * _a0, Window _a1) { return 0; }
int XVendorRelease(Display * _a0) { return 0; }
int XWidthMMOfScreen(Screen * _a0) { return 0; }
int XWriteBitmapFile(Display * _a0, const char * _a1, Pixmap _a2, unsigned int _a3, unsigned int _a4, int _a5, int _a6) { return 0; }
XOM XOpenOM(Display * _a0, struct _XrmHashBucketRec * _a1, const char * _a2, const char * _a3) { return 0; }
int XCloseOM(XOM _a0) { return 0; }
char * XSetOMValues(XOM _a0, ...) { return 0; }
char * XGetOMValues(XOM _a0, ...) { return 0; }
Display * XDisplayOfOM(XOM _a0) { return 0; }
char * XLocaleOfOM(XOM _a0) { return 0; }
XOC XCreateOC(XOM _a0, ...) { return 0; }
void XDestroyOC(XOC _a0) { (void)0; }
XOM XOMOfOC(XOC _a0) { return 0; }
char * XSetOCValues(XOC _a0, ...) { return 0; }
char * XGetOCValues(XOC _a0, ...) { return 0; }
int XFontsOfFontSet(XFontSet _a0, XFontStruct *** _a1, char *** _a2) { return 0; }
char * XBaseFontNameListOfFontSet(XFontSet _a0) { return 0; }
char * XLocaleOfFontSet(XFontSet _a0) { return 0; }
int XContextDependentDrawing(XFontSet _a0) { return 0; }
int XDirectionalDependentDrawing(XFontSet _a0) { return 0; }
int XContextualDrawing(XFontSet _a0) { return 0; }
XFontSetExtents * XExtentsOfFontSet(XFontSet _a0) { return 0; }
int XmbTextEscapement(XFontSet _a0, const char * _a1, int _a2) { return 0; }
int XwcTextEscapement(XFontSet _a0, const wchar_t * _a1, int _a2) { return 0; }
int Xutf8TextEscapement(XFontSet _a0, const char * _a1, int _a2) { return 0; }
int XmbTextExtents(XFontSet _a0, const char * _a1, int _a2, XRectangle * _a3, XRectangle * _a4) { return 0; }
int XwcTextExtents(XFontSet _a0, const wchar_t * _a1, int _a2, XRectangle * _a3, XRectangle * _a4) { return 0; }
int XmbTextPerCharExtents(XFontSet _a0, const char * _a1, int _a2, XRectangle * _a3, XRectangle * _a4, int _a5, int * _a6, XRectangle * _a7, XRectangle * _a8) { return 0; }
int XwcTextPerCharExtents(XFontSet _a0, const wchar_t * _a1, int _a2, XRectangle * _a3, XRectangle * _a4, int _a5, int * _a6, XRectangle * _a7, XRectangle * _a8) { return 0; }
int Xutf8TextPerCharExtents(XFontSet _a0, const char * _a1, int _a2, XRectangle * _a3, XRectangle * _a4, int _a5, int * _a6, XRectangle * _a7, XRectangle * _a8) { return 0; }
void XmbDrawText(Display * _a0, Drawable _a1, GC _a2, int _a3, int _a4, XmbTextItem * _a5, int _a6) { (void)0; }
void XwcDrawText(Display * _a0, Drawable _a1, GC _a2, int _a3, int _a4, XwcTextItem * _a5, int _a6) { (void)0; }
void Xutf8DrawText(Display * _a0, Drawable _a1, GC _a2, int _a3, int _a4, XmbTextItem * _a5, int _a6) { (void)0; }
void XmbDrawString(Display * _a0, Drawable _a1, XFontSet _a2, GC _a3, int _a4, int _a5, const char * _a6, int _a7) { (void)0; }
void XwcDrawString(Display * _a0, Drawable _a1, XFontSet _a2, GC _a3, int _a4, int _a5, const wchar_t * _a6, int _a7) { (void)0; }
void XmbDrawImageString(Display * _a0, Drawable _a1, XFontSet _a2, GC _a3, int _a4, int _a5, const char * _a6, int _a7) { (void)0; }
void XwcDrawImageString(Display * _a0, Drawable _a1, XFontSet _a2, GC _a3, int _a4, int _a5, const wchar_t * _a6, int _a7) { (void)0; }
void Xutf8DrawImageString(Display * _a0, Drawable _a1, XFontSet _a2, GC _a3, int _a4, int _a5, const char * _a6, int _a7) { (void)0; }
char * XGetIMValues(XIM _a0, ...) { return 0; }
char * XSetIMValues(XIM _a0, ...) { return 0; }
Display * XDisplayOfIM(XIM _a0) { return 0; }
char * XLocaleOfIM(XIM _a0) { return 0; }
wchar_t * XwcResetIC(XIC _a0) { return 0; }
char * XmbResetIC(XIC _a0) { return 0; }
char * XSetICValues(XIC _a0, ...) { return 0; }
XIM XIMOfIC(XIC _a0) { return 0; }
int XmbLookupString(XIC _a0, XKeyPressedEvent * _a1, char * _a2, int _a3, KeySym * _a4, int * _a5) { return 0; }
int XwcLookupString(XIC _a0, XKeyPressedEvent * _a1, wchar_t * _a2, int _a3, KeySym * _a4, int * _a5) { return 0; }
XVaNestedList XVaCreateNestedList(int _a0, ...) { return 0; }
int XRegisterIMInstantiateCallback(Display * _a0, struct _XrmHashBucketRec * _a1, char * _a2, char * _a3, XIDProc _a4, XPointer _a5) { return 0; }
int XUnregisterIMInstantiateCallback(Display * _a0, struct _XrmHashBucketRec * _a1, char * _a2, char * _a3, XIDProc _a4, XPointer _a5) { return 0; }
int XInternalConnectionNumbers(Display * _a0, int ** _a1, int * _a2) { return 0; }
void XProcessInternalConnection(Display * _a0, int _a1) { (void)0; }
int XAddConnectionWatch(Display * _a0, XConnectionWatchProc _a1, XPointer _a2) { return 0; }
void XRemoveConnectionWatch(Display * _a0, XConnectionWatchProc _a1, XPointer _a2) { (void)0; }
void XSetAuthorization(char * _a0, int _a1, char * _a2, int _a3) { (void)0; }
int _Xmbtowc(wchar_t * _a0, char * _a1, int _a2) { return 0; }
int _Xwctomb(char * _a0, wchar_t _a1) { return 0; }
XIconSize * XAllocIconSize(void) { return 0; }
XStandardColormap * XAllocStandardColormap(void) { return 0; }
int XClipBox(Region _a0, XRectangle * _a1) { return 0; }
Region XCreateRegion(void) { return 0; }
const char * XDefaultString(void) { return 0; }
int XDeleteContext(Display * _a0, XID _a1, XContext _a2) { return 0; }
int XDestroyRegion(Region _a0) { return 0; }
int XEmptyRegion(Region _a0) { return 0; }
int XEqualRegion(Region _a0, Region _a1) { return 0; }
int XFindContext(Display * _a0, XID _a1, XContext _a2, XPointer * _a3) { return 0; }
int XGetClassHint(Display * _a0, Window _a1, XClassHint * _a2) { return 0; }
int XGetIconSizes(Display * _a0, Window _a1, XIconSize ** _a2, int * _a3) { return 0; }
int XGetNormalHints(Display * _a0, Window _a1, XSizeHints * _a2) { return 0; }
int XGetRGBColormaps(Display * _a0, Window _a1, XStandardColormap ** _a2, int * _a3, Atom _a4) { return 0; }
int XGetSizeHints(Display * _a0, Window _a1, XSizeHints * _a2, Atom _a3) { return 0; }
int XGetStandardColormap(Display * _a0, Window _a1, XStandardColormap * _a2, Atom _a3) { return 0; }
int XGetTextProperty(Display * _a0, Window _a1, XTextProperty * _a2, Atom _a3) { return 0; }
int XGetWMClientMachine(Display * _a0, Window _a1, XTextProperty * _a2) { return 0; }
int XGetWMIconName(Display * _a0, Window _a1, XTextProperty * _a2) { return 0; }
int XGetWMName(Display * _a0, Window _a1, XTextProperty * _a2) { return 0; }
int XGetWMSizeHints(Display * _a0, Window _a1, XSizeHints * _a2, long * _a3, Atom _a4) { return 0; }
int XGetZoomHints(Display * _a0, Window _a1, XSizeHints * _a2) { return 0; }
int XIntersectRegion(Region _a0, Region _a1, Region _a2) { return 0; }
void XConvertCase(KeySym _a0, KeySym * _a1, KeySym * _a2) { (void)0; }
int XOffsetRegion(Region _a0, int _a1, int _a2) { return 0; }
int XPointInRegion(Region _a0, int _a1, int _a2) { return 0; }
Region XPolygonRegion(XPoint * _a0, int _a1, int _a2) { return 0; }
int XRectInRegion(Region _a0, int _a1, int _a2, unsigned int _a3, unsigned int _a4) { return 0; }
int XSaveContext(Display * _a0, XID _a1, XContext _a2, const char * _a3) { return 0; }
int XSetClassHint(Display * _a0, Window _a1, XClassHint * _a2) { return 0; }
int XSetIconSizes(Display * _a0, Window _a1, XIconSize * _a2, int _a3) { return 0; }
int XSetNormalHints(Display * _a0, Window _a1, XSizeHints * _a2) { return 0; }
void XSetRGBColormaps(Display * _a0, Window _a1, XStandardColormap * _a2, int _a3, Atom _a4) { (void)0; }
int XSetSizeHints(Display * _a0, Window _a1, XSizeHints * _a2, Atom _a3) { return 0; }
int XSetStandardProperties(Display * _a0, Window _a1, const char * _a2, const char * _a3, Pixmap _a4, char ** _a5, int _a6, XSizeHints * _a7) { return 0; }
void XSetWMClientMachine(Display * _a0, Window _a1, XTextProperty * _a2) { (void)0; }
void XSetWMIconName(Display * _a0, Window _a1, XTextProperty * _a2) { (void)0; }
void XSetWMName(Display * _a0, Window _a1, XTextProperty * _a2) { (void)0; }
void XmbSetWMProperties(Display * _a0, Window _a1, const char * _a2, const char * _a3, char ** _a4, int _a5, XSizeHints * _a6, XWMHints * _a7, XClassHint * _a8) { (void)0; }
void Xutf8SetWMProperties(Display * _a0, Window _a1, const char * _a2, const char * _a3, char ** _a4, int _a5, XSizeHints * _a6, XWMHints * _a7, XClassHint * _a8) { (void)0; }
void XSetWMSizeHints(Display * _a0, Window _a1, XSizeHints * _a2, Atom _a3) { (void)0; }
int XSetRegion(Display * _a0, GC _a1, Region _a2) { return 0; }
void XSetStandardColormap(Display * _a0, Window _a1, XStandardColormap * _a2, Atom _a3) { (void)0; }
int XSetZoomHints(Display * _a0, Window _a1, XSizeHints * _a2) { return 0; }
int XShrinkRegion(Region _a0, int _a1, int _a2) { return 0; }
int XSubtractRegion(Region _a0, Region _a1, Region _a2) { return 0; }
int XwcTextListToTextProperty(Display * _a0, wchar_t ** _a1, int _a2, XICCEncodingStyle _a3, XTextProperty * _a4) { return 0; }
void XwcFreeStringList(wchar_t ** _a0) { (void)0; }
int XTextPropertyToStringList(XTextProperty * _a0, char *** _a1, int * _a2) { return 0; }
int XmbTextPropertyToTextList(Display * _a0, const XTextProperty * _a1, char *** _a2, int * _a3) { return 0; }
int XwcTextPropertyToTextList(Display * _a0, const XTextProperty * _a1, wchar_t *** _a2, int * _a3) { return 0; }
int Xutf8TextPropertyToTextList(Display * _a0, const XTextProperty * _a1, char *** _a2, int * _a3) { return 0; }
int XUnionRectWithRegion(XRectangle * _a0, Region _a1, Region _a2) { return 0; }
int XUnionRegion(Region _a0, Region _a1, Region _a2) { return 0; }
int XWMGeometry(Display * _a0, int _a1, const char * _a2, const char * _a3, unsigned int _a4, XSizeHints * _a5, int * _a6, int * _a7, int * _a8, int * _a9, int * _a10) { return 0; }
int XXorRegion(Region _a0, Region _a1, Region _a2) { return 0; }
int XkbIgnoreExtension(int _a0) { return 0; }
Display * XkbOpenDisplay(char * _a0, int * _a1, int * _a2, int * _a3, int * _a4, int * _a5) { return 0; }
int XkbUseExtension(Display * _a0, int * _a1, int * _a2) { return 0; }
int XkbLibraryVersion(int * _a0, int * _a1) { return 0; }
unsigned int XkbSetXlibControls(Display * _a0, unsigned int _a1, unsigned int _a2) { return 0; }
unsigned int XkbGetXlibControls(Display * _a0) { return 0; }
unsigned int XkbXlibControlsImplemented(void) { return 0; }
void XkbSetAtomFuncs(XkbInternAtomFunc _a0, XkbGetAtomNameFunc _a1) { (void)0; }
unsigned int XkbKeysymToModifiers(Display * _a0, KeySym _a1) { return 0; }
int XkbLookupKeySym(Display * _a0, KeyCode _a1, unsigned int _a2, unsigned int * _a3, KeySym * _a4) { return 0; }
int XkbLookupKeyBinding(Display * _a0, KeySym _a1, unsigned int _a2, char * _a3, int _a4, int * _a5) { return 0; }
int XkbTranslateKeyCode(XkbDescPtr _a0, KeyCode _a1, unsigned int _a2, unsigned int * _a3, KeySym * _a4) { return 0; }
int XkbTranslateKeySym(Display * _a0, KeySym * _a1, unsigned int _a2, char * _a3, int _a4, int * _a5) { return 0; }
int XkbSetAutoRepeatRate(Display * _a0, unsigned int _a1, unsigned int _a2, unsigned int _a3) { return 0; }
int XkbGetAutoRepeatRate(Display * _a0, unsigned int _a1, unsigned int * _a2, unsigned int * _a3) { return 0; }
int XkbChangeEnabledControls(Display * _a0, unsigned int _a1, unsigned int _a2, unsigned int _a3) { return 0; }
int XkbDeviceBell(Display * _a0, Window _a1, int _a2, int _a3, int _a4, int _a5, Atom _a6) { return 0; }
int XkbForceDeviceBell(Display * _a0, int _a1, int _a2, int _a3, int _a4) { return 0; }
int XkbDeviceBellEvent(Display * _a0, Window _a1, int _a2, int _a3, int _a4, int _a5, Atom _a6) { return 0; }
int XkbBell(Display * _a0, Window _a1, int _a2, Atom _a3) { return 0; }
int XkbForceBell(Display * _a0, int _a1) { return 0; }
int XkbBellEvent(Display * _a0, Window _a1, int _a2, Atom _a3) { return 0; }
int XkbSelectEvents(Display * _a0, unsigned int _a1, unsigned int _a2, unsigned int _a3) { return 0; }
int XkbSelectEventDetails(Display * _a0, unsigned int _a1, unsigned int _a2, unsigned long _a3, unsigned long _a4) { return 0; }
void XkbNoteMapChanges(XkbMapChangesPtr _a0, XkbMapNotifyEvent * _a1, unsigned int _a2) { (void)0; }
void XkbNoteNameChanges(XkbNameChangesPtr _a0, XkbNamesNotifyEvent * _a1, unsigned int _a2) { (void)0; }
int XkbGetIndicatorState(Display * _a0, unsigned int _a1, unsigned int * _a2) { return 0; }
int XkbGetIndicatorMap(Display * _a0, unsigned long _a1, XkbDescPtr _a2) { return 0; }
int XkbSetIndicatorMap(Display * _a0, unsigned long _a1, XkbDescPtr _a2) { return 0; }
int XkbGetNamedIndicator(Display * _a0, Atom _a1, int * _a2, int * _a3, XkbIndicatorMapPtr _a4, int * _a5) { return 0; }
int XkbGetNamedDeviceIndicator(Display * _a0, unsigned int _a1, unsigned int _a2, unsigned int _a3, Atom _a4, int * _a5, int * _a6, XkbIndicatorMapPtr _a7, int * _a8) { return 0; }
int XkbSetNamedIndicator(Display * _a0, Atom _a1, int _a2, int _a3, int _a4, XkbIndicatorMapPtr _a5) { return 0; }
int XkbSetNamedDeviceIndicator(Display * _a0, unsigned int _a1, unsigned int _a2, unsigned int _a3, Atom _a4, int _a5, int _a6, int _a7, XkbIndicatorMapPtr _a8) { return 0; }
int XkbLockModifiers(Display * _a0, unsigned int _a1, unsigned int _a2, unsigned int _a3) { return 0; }
int XkbLatchModifiers(Display * _a0, unsigned int _a1, unsigned int _a2, unsigned int _a3) { return 0; }
int XkbLockGroup(Display * _a0, unsigned int _a1, unsigned int _a2) { return 0; }
int XkbLatchGroup(Display * _a0, unsigned int _a1, unsigned int _a2) { return 0; }
int XkbSetServerInternalMods(Display * _a0, unsigned int _a1, unsigned int _a2, unsigned int _a3, unsigned int _a4, unsigned int _a5) { return 0; }
int XkbSetIgnoreLockMods(Display * _a0, unsigned int _a1, unsigned int _a2, unsigned int _a3, unsigned int _a4, unsigned int _a5) { return 0; }
int XkbVirtualModsToReal(XkbDescPtr _a0, unsigned int _a1, unsigned int * _a2) { return 0; }
int XkbComputeEffectiveMap(XkbDescPtr _a0, XkbKeyTypePtr _a1, unsigned char * _a2) { return 0; }
int XkbInitCanonicalKeyTypes(XkbDescPtr _a0, unsigned int _a1, int _a2) { return 0; }
XkbDescPtr XkbAllocKeyboard(void) { return 0; }
int XkbAllocClientMap(XkbDescPtr _a0, unsigned int _a1, unsigned int _a2) { return 0; }
int XkbAllocServerMap(XkbDescPtr _a0, unsigned int _a1, unsigned int _a2) { return 0; }
void XkbFreeServerMap(XkbDescPtr _a0, unsigned int _a1, int _a2) { (void)0; }
XkbKeyTypePtr XkbAddKeyType(XkbDescPtr _a0, Atom _a1, int _a2, int _a3, int _a4) { return 0; }
int XkbAllocIndicatorMaps(XkbDescPtr _a0) { return 0; }
void XkbFreeIndicatorMaps(XkbDescPtr _a0) { (void)0; }
int XkbGetMapChanges(Display * _a0, XkbDescPtr _a1, XkbMapChangesPtr _a2) { return 0; }
int XkbRefreshKeyboardMapping(XkbMapNotifyEvent * _a0) { return 0; }
int XkbGetKeyTypes(Display * _a0, unsigned int _a1, unsigned int _a2, XkbDescPtr _a3) { return 0; }
int XkbGetKeySyms(Display * _a0, unsigned int _a1, unsigned int _a2, XkbDescPtr _a3) { return 0; }
int XkbGetKeyActions(Display * _a0, unsigned int _a1, unsigned int _a2, XkbDescPtr _a3) { return 0; }
int XkbGetKeyBehaviors(Display * _a0, unsigned int _a1, unsigned int _a2, XkbDescPtr _a3) { return 0; }
int XkbGetVirtualMods(Display * _a0, unsigned int _a1, XkbDescPtr _a2) { return 0; }
int XkbGetKeyExplicitComponents(Display * _a0, unsigned int _a1, unsigned int _a2, XkbDescPtr _a3) { return 0; }
int XkbGetKeyModifierMap(Display * _a0, unsigned int _a1, unsigned int _a2, XkbDescPtr _a3) { return 0; }
int XkbGetKeyVirtualModMap(Display * _a0, unsigned int _a1, unsigned int _a2, XkbDescPtr _a3) { return 0; }
int XkbAllocControls(XkbDescPtr _a0, unsigned int _a1) { return 0; }
void XkbFreeControls(XkbDescPtr _a0, unsigned int _a1, int _a2) { (void)0; }
int XkbGetControls(Display * _a0, unsigned long _a1, XkbDescPtr _a2) { return 0; }
int XkbSetControls(Display * _a0, unsigned long _a1, XkbDescPtr _a2) { return 0; }
void XkbNoteControlsChanges(XkbControlsChangesPtr _a0, XkbControlsNotifyEvent * _a1, unsigned int _a2) { (void)0; }
int XkbAllocCompatMap(XkbDescPtr _a0, unsigned int _a1, unsigned int _a2) { return 0; }
void XkbFreeCompatMap(XkbDescPtr _a0, unsigned int _a1, int _a2) { (void)0; }
int XkbGetCompatMap(Display * _a0, unsigned int _a1, XkbDescPtr _a2) { return 0; }
int XkbSetCompatMap(Display * _a0, unsigned int _a1, XkbDescPtr _a2, int _a3) { return 0; }
int XkbAllocNames(XkbDescPtr _a0, unsigned int _a1, int _a2, int _a3) { return 0; }
int XkbGetNames(Display * _a0, unsigned int _a1, XkbDescPtr _a2) { return 0; }
int XkbSetNames(Display * _a0, unsigned int _a1, unsigned int _a2, unsigned int _a3, XkbDescPtr _a4) { return 0; }
int XkbChangeNames(Display * _a0, XkbDescPtr _a1, XkbNameChangesPtr _a2) { return 0; }
void XkbFreeNames(XkbDescPtr _a0, unsigned int _a1, int _a2) { (void)0; }
int XkbSetMap(Display * _a0, unsigned int _a1, XkbDescPtr _a2) { return 0; }
int XkbChangeMap(Display * _a0, XkbDescPtr _a1, XkbMapChangesPtr _a2) { return 0; }
int XkbGetDetectableAutoRepeat(Display * _a0, int * _a1) { return 0; }
int XkbSetAutoResetControls(Display * _a0, unsigned int _a1, unsigned int * _a2, unsigned int * _a3) { return 0; }
int XkbGetAutoResetControls(Display * _a0, unsigned int * _a1, unsigned int * _a2) { return 0; }
int XkbSetPerClientControls(Display * _a0, unsigned int _a1, unsigned int * _a2) { return 0; }
int XkbGetPerClientControls(Display * _a0, unsigned int * _a1) { return 0; }
int XkbCopyKeyType(XkbKeyTypePtr _a0, XkbKeyTypePtr _a1) { return 0; }
int XkbCopyKeyTypes(XkbKeyTypePtr _a0, XkbKeyTypePtr _a1, int _a2) { return 0; }
int XkbResizeKeyType(XkbDescPtr _a0, int _a1, int _a2, int _a3, int _a4) { return 0; }
KeySym * XkbResizeKeySyms(XkbDescPtr _a0, int _a1, int _a2) { return 0; }
XkbAction * XkbResizeKeyActions(XkbDescPtr _a0, int _a1, int _a2) { return 0; }
int XkbChangeTypesOfKey(XkbDescPtr _a0, int _a1, int _a2, unsigned int _a3, int * _a4, XkbMapChangesPtr _a5) { return 0; }
int XkbChangeKeycodeRange(XkbDescPtr _a0, int _a1, int _a2, XkbChangesPtr _a3) { return 0; }
XkbComponentListPtr XkbListComponents(Display * _a0, unsigned int _a1, XkbComponentNamesPtr _a2, int * _a3) { return 0; }
void XkbFreeComponentList(XkbComponentListPtr _a0) { (void)0; }
XkbDescPtr XkbGetKeyboard(Display * _a0, unsigned int _a1, unsigned int _a2) { return 0; }
XkbDescPtr XkbGetKeyboardByName(Display * _a0, unsigned int _a1, XkbComponentNamesPtr _a2, unsigned int _a3, unsigned int _a4, int _a5) { return 0; }
int XkbKeyTypesForCoreSymbols(XkbDescPtr _a0, int _a1, KeySym * _a2, unsigned int _a3, int * _a4, KeySym * _a5) { return 0; }
int XkbApplyCompatMapToKey(XkbDescPtr _a0, KeyCode _a1, XkbChangesPtr _a2) { return 0; }
int XkbUpdateMapFromCore(XkbDescPtr _a0, KeyCode _a1, int _a2, int _a3, KeySym * _a4, XkbChangesPtr _a5) { return 0; }
XkbDeviceLedInfoPtr XkbAddDeviceLedInfo(XkbDeviceInfoPtr _a0, unsigned int _a1, unsigned int _a2) { return 0; }
int XkbResizeDeviceButtonActions(XkbDeviceInfoPtr _a0, unsigned int _a1) { return 0; }
XkbDeviceInfoPtr XkbAllocDeviceInfo(unsigned int _a0, unsigned int _a1, unsigned int _a2) { return 0; }
void XkbFreeDeviceInfo(XkbDeviceInfoPtr _a0, unsigned int _a1, int _a2) { (void)0; }
void XkbNoteDeviceChanges(XkbDeviceChangesPtr _a0, XkbExtensionDeviceNotifyEvent * _a1, unsigned int _a2) { (void)0; }
XkbDeviceInfoPtr XkbGetDeviceInfo(Display * _a0, unsigned int _a1, unsigned int _a2, unsigned int _a3, unsigned int _a4) { return 0; }
int XkbGetDeviceInfoChanges(Display * _a0, XkbDeviceInfoPtr _a1, XkbDeviceChangesPtr _a2) { return 0; }
int XkbGetDeviceButtonActions(Display * _a0, XkbDeviceInfoPtr _a1, int _a2, unsigned int _a3, unsigned int _a4) { return 0; }
int XkbGetDeviceLedInfo(Display * _a0, XkbDeviceInfoPtr _a1, unsigned int _a2, unsigned int _a3, unsigned int _a4) { return 0; }
int XkbSetDeviceInfo(Display * _a0, unsigned int _a1, XkbDeviceInfoPtr _a2) { return 0; }
int XkbChangeDeviceInfo(Display * _a0, XkbDeviceInfoPtr _a1, XkbDeviceChangesPtr _a2) { return 0; }
int XkbSetDeviceLedInfo(Display * _a0, XkbDeviceInfoPtr _a1, unsigned int _a2, unsigned int _a3, unsigned int _a4) { return 0; }
int XkbSetDeviceButtonActions(Display * _a0, XkbDeviceInfoPtr _a1, unsigned int _a2, unsigned int _a3) { return 0; }
char XkbToControl(char _a0) { return 0; }
int XkbSetDebuggingFlags(Display * _a0, unsigned int _a1, unsigned int _a2, char * _a3, unsigned int _a4, unsigned int _a5, unsigned int * _a6, unsigned int * _a7) { return 0; }
int XkbApplyVirtualModChanges(XkbDescPtr _a0, unsigned int _a1, XkbChangesPtr _a2) { return 0; }
int XkbUpdateActionVirtualMods(XkbDescPtr _a0, XkbAction * _a1, unsigned int _a2) { return 0; }
void XkbUpdateKeyTypeVirtualMods(XkbDescPtr _a0, XkbKeyTypePtr _a1, unsigned int _a2, XkbChangesPtr _a3) { (void)0; }
