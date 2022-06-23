package com.auth0.sample

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.auth0.android.Auth0
import com.auth0.android.authentication.AuthenticationAPIClient
import com.auth0.android.authentication.AuthenticationException
import com.auth0.android.authentication.storage.CredentialsManager
import com.auth0.android.authentication.storage.CredentialsManagerException
import com.auth0.android.authentication.storage.SharedPreferencesStorage
import com.auth0.android.callback.Callback
import com.auth0.android.management.ManagementException
import com.auth0.android.management.UsersAPIClient
import com.auth0.android.provider.WebAuthProvider
import com.auth0.android.request.DefaultClient
import com.auth0.android.result.Credentials
import com.auth0.android.result.UserProfile
import com.auth0.sample.databinding.FragmentDatabaseLoginBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class DatabaseLoginFragment : Fragment() {

    private var saveCreds = false
    private val scope = "openid profile email read:current_user update:current_user_metadata"

    private val account: Auth0 by lazy {
        // -- REPLACE this credentials with your own Auth0 app credentials!
        val account = Auth0(
            getString(R.string.com_auth0_client_id),
            getString(R.string.com_auth0_domain)
        )
        // Only enable network traffic logging on production environments!
        account.networkingClient = DefaultClient(enableLogging = true)
        account
    }

    private val audience: String by lazy {
        "https://${getString(R.string.com_auth0_domain)}/api/v2/"
    }

    private val authenticationApiClient: AuthenticationAPIClient by lazy {
        AuthenticationAPIClient(account)
    }

    private val credentialsManager: CredentialsManager by lazy {
        val storage = SharedPreferencesStorage(requireContext())
        val manager = CredentialsManager(authenticationApiClient, storage)
        manager
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentDatabaseLoginBinding.inflate(inflater, container, false)
        binding.buttonLogin.setOnClickListener {
            val email = binding.textEmail.text.toString()
            val password = binding.textPassword.text.toString()
            dbLogin(email, password)
        }
        binding.buttonLoginAsync.setOnClickListener {
            GlobalScope.launch(Dispatchers.Main) {
                val email = binding.textEmail.text.toString()
                val password = binding.textPassword.text.toString()
                dbLoginAsync(email, password)
            }
        }
        binding.buttonWebAuth.setOnClickListener {
            webAuth()
        }
        binding.buttonWebLogout.setOnClickListener {
            webLogout()
        }
        binding.saveCreds.setOnCheckedChangeListener { compoundButton, b ->
            saveCreds = binding.saveCreds.isChecked
        }
        binding.deleteCreds.setOnClickListener {
            deleteCreds()
        }
        binding.getCreds.setOnClickListener {
            getCreds()
        }
        binding.getCredsAsync.setOnClickListener {
            GlobalScope.launch(Dispatchers.Main) {
                getCredsAsync()
            }
        }
        binding.getProfile.setOnClickListener {
            getProfile()
        }
        binding.getProfileAsync.setOnClickListener {
            GlobalScope.launch(Dispatchers.Main) {
                getProfileAsync()
            }
        }
        binding.updateMeta.setOnClickListener {
            updateMeta()
        }
        binding.updateMetaAsync.setOnClickListener {
            GlobalScope.launch(Dispatchers.Main) {
                updateMetaAsync()
            }
        }
        return binding.root
    }

    private suspend fun dbLoginAsync(email: String, password: String) {
        try {
            val result = authenticationApiClient.login(email, password, "Username-Password-Authentication")
                .validateClaims()
                .addParameter("scope", scope)
                .addParameter("audience", audience)
                .await()
            if (saveCreds) {
                credentialsManager.saveCredentials(result)
            }
            Snackbar.make(
                requireView(),
                "Hello ${result.user.name}",
                Snackbar.LENGTH_LONG
            ).show()
        } catch (error: AuthenticationException) {
            Snackbar.make(requireView(), error.getDescription(), Snackbar.LENGTH_LONG)
                .show()
        }
    }

    private fun dbLogin(email: String, password: String) {
        authenticationApiClient.login(email, password, "Username-Password-Authentication")
            .validateClaims()
            .addParameter("scope", scope)
            .addParameter("audience", audience)
            //Additional customization to the request goes here
            .start(object : Callback<Credentials, AuthenticationException> {
                override fun onFailure(error: AuthenticationException) {
                    Snackbar.make(requireView(), error.getDescription(), Snackbar.LENGTH_LONG)
                        .show()
                }

                override fun onSuccess(result: Credentials) {
                    if (saveCreds) {
                        credentialsManager.saveCredentials(result)
                    }
                    Snackbar.make(
                        requireView(),
                        "Hello ${result.user.name}",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            })
    }

    private fun webAuth() {
        WebAuthProvider.login(account)
            .withScheme(getString(R.string.com_auth0_scheme))
            .withAudience(audience)
            .withScope(scope)
            .start(requireContext(), object : Callback<Credentials, AuthenticationException> {
                override fun onSuccess(result: Credentials) {
                    if (saveCreds) {
                        credentialsManager.saveCredentials(result)
                    }
                    Snackbar.make(
                        requireView(),
                        "Hello ${result.user.name}",
                        Snackbar.LENGTH_LONG
                    ).show()
                }

                override fun onFailure(error: AuthenticationException) {
                    val message =
                        if (error.isCanceled) "Browser was closed" else error.getDescription()
                    Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show()
                }
            })
    }

    private fun webLogout() {
        WebAuthProvider.logout(account)
            .withScheme(getString(R.string.com_auth0_scheme))
            .start(requireContext(), object : Callback<Void?, AuthenticationException> {
                override fun onSuccess(result: Void?) {
                    Snackbar.make(
                        requireView(),
                        "Logged out",
                        Snackbar.LENGTH_LONG
                    ).show()
                }

                override fun onFailure(error: AuthenticationException) {
                    val message =
                        if (error.isCanceled) "Browser was closed" else error.getDescription()
                    Snackbar.make(requireView(), message, Snackbar.LENGTH_LONG).show()
                }

            })
    }

    private fun deleteCreds() {
        credentialsManager.clearCredentials()
    }

    private fun getCreds() {
        credentialsManager.getCredentials(object : Callback<Credentials, CredentialsManagerException> {
            override fun onSuccess(credentials: Credentials) {
                Snackbar.make(
                    requireView(),
                    "Got credentials for ${credentials.user.name}",
                    Snackbar.LENGTH_LONG
                ).show()
            }

            override fun onFailure(error: CredentialsManagerException) {
                Snackbar.make(requireView(), "${error.message}", Snackbar.LENGTH_LONG).show()
            }
        })
    }

    private suspend fun getCredsAsync() {
        try {
            val credentials = credentialsManager.awaitCredentials()
            Snackbar.make(
                requireView(),
                "Got credentials for ${credentials.user.name}",
                Snackbar.LENGTH_LONG
            ).show()
        } catch (error: CredentialsManagerException) {
            Snackbar.make(requireView(), "${error.message}", Snackbar.LENGTH_LONG).show()
        }
    }

    private fun getProfile() {
        credentialsManager.getCredentials(object : Callback<Credentials, CredentialsManagerException> {
            override fun onSuccess(result: Credentials) {
                val users = UsersAPIClient(account, result.accessToken)
                users.getProfile(result.user.getId()!!)
                    .start(object: Callback<UserProfile, ManagementException> {
                        override fun onFailure(error: ManagementException) {
                            Snackbar.make(requireView(), error.getDescription(), Snackbar.LENGTH_LONG).show()
                        }
                        override fun onSuccess(result: UserProfile) {
                            Snackbar.make(
                                requireView(),
                                "Got profile for ${result.name}",
                                Snackbar.LENGTH_LONG
                            ).show()
                        }
                })
            }
            override fun onFailure(error: CredentialsManagerException) {
                Snackbar.make(requireView(), "${error.message}", Snackbar.LENGTH_LONG).show()
            }
        })
    }

    private suspend fun getProfileAsync() {
        try {
            val credentials = credentialsManager.awaitCredentials()
            val users = UsersAPIClient(account, credentials.accessToken)
            val user = users.getProfile(credentials.user.getId()!!).await()
            Snackbar.make(
                requireView(),
                "Got profile for ${user.name}",
                Snackbar.LENGTH_LONG
            ).show()
        } catch (error: CredentialsManagerException) {
            Snackbar.make(requireView(), "${error.message}", Snackbar.LENGTH_LONG).show()
        } catch (error: ManagementException) {
            Snackbar.make(requireView(), error.getDescription(), Snackbar.LENGTH_LONG).show()
        }
    }

    private fun updateMeta() {
        val metadata = mapOf(
            "random" to (0..100).random(),
        )

        credentialsManager.getCredentials(object : Callback<Credentials, CredentialsManagerException> {
            override fun onSuccess(result: Credentials) {
                val users = UsersAPIClient(account, result.accessToken)
                users.updateMetadata(result.user.getId()!!, metadata)
                    .start(object: Callback<UserProfile, ManagementException> {
                        override fun onFailure(error: ManagementException) {
                            Snackbar.make(requireView(), error.getDescription(), Snackbar.LENGTH_LONG).show()
                        }
                        override fun onSuccess(result: UserProfile) {
                            Snackbar.make(
                                requireView(),
                                "Updated metadata for ${result.name} to ${result.getUserMetadata()}",
                                Snackbar.LENGTH_LONG
                            ).show()
                        }
                })
            }
            override fun onFailure(error: CredentialsManagerException) {
                Snackbar.make(requireView(), "${error.message}", Snackbar.LENGTH_LONG).show()
            }
        })
    }

    private suspend fun updateMetaAsync() {
        val metadata = mapOf(
            "random" to (0..100).random(),
        )

        try {
            val credentials = credentialsManager.awaitCredentials()
            val users = UsersAPIClient(account, credentials.accessToken)
            val user = users.updateMetadata(credentials.user.getId()!!, metadata).await()
            Snackbar.make(
                requireView(),
                "Updated metadata for ${user.name} to ${user.getUserMetadata()}",
                Snackbar.LENGTH_LONG
            ).show()
        } catch (error: CredentialsManagerException) {
            Snackbar.make(requireView(), "${error.message}", Snackbar.LENGTH_LONG).show()
        } catch (error: ManagementException) {
            Snackbar.make(requireView(), error.getDescription(), Snackbar.LENGTH_LONG).show()
        }
    }
}