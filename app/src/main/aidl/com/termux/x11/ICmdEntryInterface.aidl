package com.termux.x11;
interface ICmdEntryInterface {
    ParcelFileDescriptor getXConnection();
    ParcelFileDescriptor getLogcatOutput();
}