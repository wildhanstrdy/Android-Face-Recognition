package com.thelazybattley.facedetection

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.thelazybattley.facedetection.databinding.ActivityMainBinding
import com.thelazybattley.facedetection.fragments.PeopleListFragment
import com.thelazybattley.facedetection.fragments.PreviewViewFragment

class MainActivity : FragmentActivity() {

    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS =
            mutableListOf(
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val peopleListFragment = PeopleListFragment()
        val previewViewFragment = PreviewViewFragment()

        if (!isAllPermissionGranted()) {
            ActivityCompat.requestPermissions(
                this,
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }

        binding.bnvActions.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.takepicture -> setCurrentFragment(previewViewFragment)
                R.id.scan -> setCurrentFragment(previewViewFragment)
                R.id.people -> setCurrentFragment(peopleListFragment)
            }
            true
        }
        binding.bnvActions.selectedItemId = R.id.takepicture
    }

    private fun isAllPermissionGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            /* context = */ this, /* permission = */ it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    private fun setCurrentFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction().apply {
            replace(R.id.fcv_fragment, fragment)
            commit()
        }
    }
}
