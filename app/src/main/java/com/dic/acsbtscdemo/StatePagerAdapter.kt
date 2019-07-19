package com.dic.acsbtscdemo

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentStatePagerAdapter

class StatePagerAdapter(fm:FragmentManager):FragmentStatePagerAdapter(fm){

    private val mFragmentList = ArrayList<Fragment>()
    private val mFragmentTitleList = ArrayList<String>()

    override fun getItem(p0: Int): Fragment{
        return mFragmentList[p0]
    }

    override fun getCount(): Int {
        return mFragmentList.count()
    }

    fun addFragment(fragment:Fragment, title:String){
        mFragmentList.add(fragment)
        mFragmentTitleList.add(title)
    }


}