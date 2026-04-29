<?php

namespace App\Form;

use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\OptionsResolver\OptionsResolver;

class TransferPointsType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        $builder
            ->add('recipientEmail', \Symfony\Component\Form\Extension\Core\Type\EmailType::class, [
                'label' => 'Recipient Email',
                'attr' => ['class' => 'form-input', 'placeholder' => 'Enter student email'],
                'constraints' => [
                    new \Symfony\Component\Validator\Constraints\NotBlank(),
                    new \Symfony\Component\Validator\Constraints\Email()
                ]
            ])
            ->add('amount', \Symfony\Component\Form\Extension\Core\Type\IntegerType::class, [
                'label' => 'Amount (Points)',
                'attr' => ['class' => 'form-input', 'min' => 1, 'placeholder' => '10'],
                'constraints' => [
                    new \Symfony\Component\Validator\Constraints\NotBlank(),
                    new \Symfony\Component\Validator\Constraints\Positive([
                        'message' => 'The transfer amount must be positive.'
                    ])
                ]
            ])
        ;
    }

    public function configureOptions(OptionsResolver $resolver): void
    {
        $resolver->setDefaults([
            // Configure your form options here
        ]);
    }
}
