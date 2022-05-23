package com.example.firebaseapp

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.firebaseapp.databinding.ActivityListaDeComprasBinding
import com.example.firebaseapp.databinding.ItemProdutoBinding
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityListaDeComprasBinding
    lateinit var database: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListaDeComprasBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tratarLogin()

        binding.fab.setOnClickListener{
            novoItem()
        }
    }

    fun novoItem(){
        val caixaTexo = EditText(this)
        caixaTexo.hint = "Nome do Item"

        AlertDialog.Builder(this)
            .setTitle("Novo Item")
            .setView(caixaTexo)
            .setPositiveButton("Adicionar"){ dialog, button ->
                val prod = Produto(nome = caixaTexo.text.toString())
                val novoNo = database.child("produtos").push()
                novoNo.key?.let{
                    prod.id = it
                }
                novoNo.setValue(prod)
            }
            .create()
            .show()
    }

    fun tratarLogin(){

        if(FirebaseAuth.getInstance().currentUser != null){
            Toast.makeText(this, "Logado", Toast.LENGTH_SHORT).show()
            configurarFirebase()
        }else{
            val providers = arrayListOf(AuthUI.IdpConfig.EmailBuilder().build())
            val i = AuthUI.getInstance().createSignInIntentBuilder().setAvailableProviders(providers).build()

            startActivityForResult(i, 1)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == 1 && resultCode == RESULT_OK){
            Toast.makeText(this, "Autenticado", Toast.LENGTH_LONG).show()
            configurarFirebase()
        }else{
            finishAffinity()
        }
    }

    fun configurarFirebase(){
        val usuario = FirebaseAuth.getInstance().currentUser

        if (usuario != null){
            database = FirebaseDatabase.getInstance().reference.child(usuario.uid)

            val databaseListener = object :ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    tratarDadosProduto(snapshot)
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(applicationContext, "Não é possível acessar o servidor", Toast.LENGTH_LONG).show()

                    Log.e("MainActivity", "onCancelled", error.toException())
                }
            }
            database.child("produtos").addValueEventListener(databaseListener)
        }
    }

    fun tratarDadosProduto(snapshot: DataSnapshot){
        val listaProdutos = arrayListOf<Produto>()

        snapshot.children.forEach{ it ->
            val produto = it.getValue(Produto::class.java)
            produto?.let {
                listaProdutos.add(it)
            }
        }
        atualizarTela(listaProdutos)
    }

    fun atualizarTela(listaProdutos: List<Produto>){
        binding.container.removeAllViews()

        listaProdutos.forEach{
            val itemBinding = ItemProdutoBinding.inflate(layoutInflater)

            itemBinding.checkComprado.isChecked = it.comprado
            itemBinding.textNome.text = it.nome

            binding.container.addView(itemBinding.root)
        }
    }
}